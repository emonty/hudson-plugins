package hudson.plugins.active_directory;

import com.sun.jndi.ldap.LdapCtxFactory;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm.DesciprotrImpl;
import hudson.security.GroupDetails;
import hudson.security.SecurityRealm;
import hudson.security.UserMayOrMayNotExistException;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;

/**
 * {@link AuthenticationProvider} with Active Directory, through LDAP.
 *
 * @author Kohsuke Kawaguchi
 * @author James Nord
 */
public class ActiveDirectoryUnixAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider
    implements UserDetailsService, GroupDetailsService {

    private final String[] domainNames;
    private final String site;

    public ActiveDirectoryUnixAuthenticationProvider(String domainName, String site) {
        this.domainNames = domainName.split(",");
        this.site = site;
    }

    /**
     * We'd like to implement {@link UserDetailsService} ideally, but in short of keeping the manager user/password,
     * we can't do so. In Active Directory authentication, we should support SPNEGO/Kerberos and
     * that should eliminate the need for the "remember me" service.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        throw new UsernameNotFoundException("Active-directory plugin doesn't support user retrieval");
    }

    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // active directory authentication is not by comparing clear text password,
        // so there's nothing to do here.
    }

    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        UserDetails userDetails = null;
        for (String domainName : domainNames) {
            try {
                userDetails = retrieveUser(username, authentication, domainName);
            }
            catch (BadCredentialsException bce) {
                LOGGER.log(Level.WARNING,"Credential exception tying to authenticate against " + domainName + " domain",bce);
            }
            if (userDetails != null) {
                break;
            }
        }
        if (userDetails == null) {
            LOGGER.log(Level.WARNING,"Exhausted all configured domains and could not authenticat against any.");
            throw new BadCredentialsException("Either no such user '"+username+"' or incorrect password");
        }
        return userDetails;
    }
    
    private UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication, String domainName) throws AuthenticationException {
        // when we use custom socket factory below, every LDAP operations result in a classloading via context classloader,
        // so we need it to resolve.
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            String password = null;
            if(authentication!=null)
                password = (String) authentication.getCredentials();

            String principalName = getPrincipalName(username, domainName);
            String id = principalName.substring(0, principalName.indexOf('@'));

            List<SocketInfo> ldapServers;
            try {
                ldapServers = DesciprotrImpl.INSTANCE.obtainLDAPServer(domainName,site);
            } catch (NamingException e) {
                LOGGER.log(Level.WARNING,"Failed to find the LDAP service",e);
                throw new AuthenticationServiceException("Failed to find the LDAP service for the domain "+domainName,e);
            }

            DirContext context = bind(principalName, password, ldapServers);

            try {
                // locate this user's record
                SearchControls controls = new SearchControls();
                controls.setSearchScope(SUBTREE_SCOPE);
                NamingEnumeration<SearchResult> renum = context.search(toDC(domainName),"(& (userPrincipalName={0})(objectClass=user))",
                        new Object[]{id}, controls);
                if(!renum.hasMore()) {
                    // failed to find it. Fall back to sAMAccountName.
                    // see http://www.nabble.com/Re%3A-Hudson-AD-plug-in-td21428668.html
                    renum = context.search(toDC(domainName),"(& (sAMAccountName={0})(objectClass=user))",
                            new Object[]{id},controls);
                    if(!renum.hasMore()) {
                        throw new BadCredentialsException("Authentication was successful but cannot locate the user information for "+username);
                    }
                }
                SearchResult result = renum.next();


                Attribute memberOf = result.getAttributes().get("memberOf");
                Set<GrantedAuthority> groups = resolveGroups(memberOf, context);
                groups.add(SecurityRealm.AUTHENTICATED_AUTHORITY);

                context.close();

                return new ActiveDirectoryUserDetail(
                    id, password,
                    true, true, true, true,
                    groups.toArray(new GrantedAuthority[groups.size()])
                );
            } catch (NamingException e) {
                LOGGER.log(Level.WARNING,"Failed to retrieve user information for "+username,e);
                throw new BadCredentialsException("Failed to retrieve user information for "+username,e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    /**
     * Binds to the server using the specified username/password.
     * <p>
     * In a real deployment, often there are servers that don't respond or otherwise broken,
     * so try all the servers.
     */
    private DirContext bind(String principalName, String password, List<SocketInfo> ldapServers) {
        // in a AD forest, it'd be mighty nice to be able to login as "joe" as opposed to "joe@europe",
        // but the bind operation doesn't appear to allow me to do so.
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put(Context.SECURITY_PRINCIPAL, principalName);
        props.put(Context.SECURITY_CREDENTIALS,password);
        props.put(Context.REFERRAL, "follow");
        // specifying custom socket factory requires a custom classloader.
        props.put("java.naming.ldap.factory.socket", TrustAllSocketFactory.class.getName());


        NamingException error = null;
        for (SocketInfo ldapServer : ldapServers) {
            try {
                return LdapCtxFactory.getLdapCtxInstance("ldaps://" + ldapServer + '/', props); // worked
            } catch (NamingException e) {
                LOGGER.log(Level.WARNING,"Failed to bind to "+ldapServer,e);
                error = e; // retry
            }
        }

        // if all the attempts failed
        throw new BadCredentialsException("Either no such user '"+principalName+"' or incorrect password",error);
    }

    /**
     * Returns the full user principal name of the form "joe@europe.contoso.com".
     * 
     * If people type in 'foo@bar' or 'bar\\foo', it should be treated as 'foo@bar.acme.org'
     */
    private String getPrincipalName(String username, String domainName) {
        String principalName;
        int slash = username.indexOf('\\');
        if (slash>0) {
            principalName = username.substring(slash+1)+'@'+username.substring(0,slash)+'.'+domainName;
        } else
        if (username.contains("@"))
            principalName = username + '.' + domainName;
        else
            principalName = username + '@' + domainName;
        return principalName;
    }

    private Set<GrantedAuthority> resolveGroups(Attribute memberOf, DirContext context) throws NamingException {
        Set<GrantedAuthority> groups = new HashSet<GrantedAuthority>();
        LinkedList<Attribute> membershipList = new LinkedList<Attribute>();
        membershipList.add(memberOf);
        while (!membershipList.isEmpty()) {
            Attribute memberships = membershipList.removeFirst();
            if (memberships != null) {
                for (int i=0; i < memberships.size() ; i++) {
                    Attributes atts = context.getAttributes("\"" + memberships.get(i) + '"', 
                                                            new String[] {"CN", "memberOf"});
                    Attribute cn = atts.get("CN");
                    if (groups.add(new GrantedAuthorityImpl(cn.get().toString()))) {
                        Attribute members = atts.get("memberOf");
                        if (members != null) {
                            membershipList.add(members);
                        }
                    }
                }
            }
        }
        return groups;
    }
    
    private static String toDC(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if(token.length()==0)   continue;   // defensive check
            if(buf.length()>0)  buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }

    private static final Logger LOGGER = Logger.getLogger(ActiveDirectoryUnixAuthenticationProvider.class.getName());

	public GroupDetails loadGroupByGroupname(String groupname) {
		throw new UserMayOrMayNotExistException(groupname);
	}
}
