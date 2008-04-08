package hudson.plugins.deploy;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.DescriptorList;
import net.sf.json.JSONObject;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;
import org.codehaus.cargo.generic.deployer.DeployerFactory;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Deploys WAR to a continer.
 * 
 * @author Kohsuke Kawaguchi
 */
public class DeployPublisher extends Publisher implements Serializable {
    public final ContainerAdapter adapter;

    public final String war;

    public DeployPublisher(ContainerAdapter adapter, String war) {
        this.adapter = adapter;
        this.war = war;
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        FilePath warFile = build.getParent().getWorkspace().child(this.war);
        if(!adapter.redeploy(warFile,build,launcher,listener))
            build.setResult(Result.FAILURE);

        return true;
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private DescriptorImpl() {
            super(DeployPublisher.class);
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new DeployPublisher(
                ContainerAdapter.LIST.newInstanceFromRadioList(formData),
                formData.getString("war"));
        }

        public String getDisplayName() {
            return Messages.DeployPublisher_DisplayName();
        }

        public DescriptorList<ContainerAdapter> getContainerAdapters() {
            return ContainerAdapter.LIST;
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }

    private static final long serialVersionUID = 1L;
}
