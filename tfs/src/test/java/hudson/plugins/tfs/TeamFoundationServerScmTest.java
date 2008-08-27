package hudson.plugins.tfs;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import org.junit.After;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class TeamFoundationServerScmTest {

    private FilePath workspace;

    @After public void tearDown() throws Exception {
        if (workspace != null) {
            workspace.deleteRecursive();
            workspace = null;
        }
    }
    
    @Test
    public void assertWorkspaceNameReplacesJobName() {
        AbstractProject<?, ?> project = mock(AbstractProject.class);
        stub(project.getName()).toReturn("ThisIsAJob");
        
        TeamFoundationServerScm scm = new TeamFoundationServerScm(null, null, ".", false, "erik_${JOB_NAME}", "user", "password");
        assertEquals("Workspace name was incorrect", "erik_ThisIsAJob", scm.getNormalizedWorkspaceName(project, mock(Launcher.class)));
    }
    
    @Test 
    public void assertDoUsernameCheckRegexWorks() {
        assertFalse("redsolo".matches(TeamFoundationServerScm.DescriptorImpl.DOMAIN_SLASH_USER_REGEX));
        assertTrue("snd\\redsolo".matches(TeamFoundationServerScm.DescriptorImpl.DOMAIN_SLASH_USER_REGEX));
        assertFalse("redsolo".matches(TeamFoundationServerScm.DescriptorImpl.USER_AT_DOMAIN_REGEX));
        assertTrue("redsolo@snd".matches(TeamFoundationServerScm.DescriptorImpl.USER_AT_DOMAIN_REGEX));
    }
    
    @Test 
    public void assertDoProjectPathCheckRegexWorks() {
        assertFalse("Project path regex matched an invalid project path", "tfsandbox".matches(TeamFoundationServerScm.DescriptorImpl.PROJECT_PATH_REGEX));
        assertFalse("Project path regex matched an invalid project path", "tfsandbox/with/sub/pathes".matches(TeamFoundationServerScm.DescriptorImpl.PROJECT_PATH_REGEX));
        assertFalse("Project path regex matched an invalid project path", "tfsandbox$/with/sub/pathes".matches(TeamFoundationServerScm.DescriptorImpl.PROJECT_PATH_REGEX));
        assertTrue("Project path regex did not match a valid project path", "$/tfsandbox".matches(TeamFoundationServerScm.DescriptorImpl.PROJECT_PATH_REGEX));
        assertTrue("Project path regex did not match a valid project path", "$/tfsandbox/path with space/subpath".matches(TeamFoundationServerScm.DescriptorImpl.PROJECT_PATH_REGEX));
    }
    
    @Test 
    public void assertDoWorkspaceNameCheckRegexWorks() {
        assertFalse("Workspace name regex matched an invalid workspace name", "work space ".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "work.space.".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "work*space".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "work/space".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "work\"space".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "workspace*".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "workspace/".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertFalse("Workspace name regex matched an invalid workspace name", "workspace\"".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertTrue("Workspace name regex dit not match an invalid workspace name", "work.space".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
        assertTrue("Workspace name regex dit not match an invalid workspace name", "work space".matches(TeamFoundationServerScm.DescriptorImpl.WORKSPACE_NAME_REGEX));
    }
    
    @Test
    public void assertDefaultValueIsUsedForEmptyLocalPath() {
        TeamFoundationServerScm scm = new TeamFoundationServerScm("serverurl", "projectpath", "", false, "workspace", "user", "password");
        assertEquals("Default value for work folder was incorrect", ".", scm.getLocalPath());
    }
    
    @Test
    public void assertDefaultValueIsUsedForEmptyWorkspaceName() {
        TeamFoundationServerScm scm = new TeamFoundationServerScm("serverurl", "projectpath", ".", false, "", "user", "password");
        assertEquals("Default value for workspace was incorrect", "Hudson-${JOB_NAME}", scm.getWorkspaceName());
    }
    
    @Test
    public void assertGetModuleRootReturnsWorkFolder() throws Exception {
        workspace = Util.createTempFilePath();
        TeamFoundationServerScm scm = new TeamFoundationServerScm("serverurl", "projectpath", "workfolder", false, "", "user", "password");
        FilePath moduleRoot = scm.getModuleRoot(workspace);
        assertEquals("Name for module root was incorrect", "workfolder", moduleRoot.getName());
        assertEquals("The parent for module root was incorrect", workspace.getName(), moduleRoot.getParent().getName());
    }
    
    @Test
    public void assertGetModuleRootWorksForDotWorkFolder() throws Exception {
        workspace = Util.createTempFilePath();
        TeamFoundationServerScm scm = new TeamFoundationServerScm("serverurl", "projectpath", ".", false, "", "user", "password");
        FilePath moduleRoot = scm.getModuleRoot(workspace);
        assertTrue("The module root was reported as not existing even if its virtually the same as workspace",
                moduleRoot.exists());
        assertEquals("The module root was not the same as workspace", moduleRoot.lastModified(), workspace.lastModified());
    }
    
    @Test
    public void assertWorkspaceNameIsAddedToEnvVars() throws Exception {
        TeamFoundationServerScm scm = new TeamFoundationServerScm("serverurl", "projectpath", ".", false, "WORKSPACE_SAMPLE", "user", "password");
        scm.getNormalizedWorkspaceName(mock(AbstractProject.class), mock(Launcher.class));
        
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(mock(AbstractBuild.class), env );        
        assertEquals("The workspace name was incorrect", "WORKSPACE_SAMPLE", env.get(TeamFoundationServerScm.WORKSPACE_ENV_STR));
    }
    
    @Test
    public void assertWorksfolderPathIsAddedToEnvVars() throws Exception {
        TeamFoundationServerScm scm = new TeamFoundationServerScm("serverurl", "projectpath", "PATH", false, "WORKSPACE_SAMPLE", "user", "password");
        
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/this/is/a");
        scm.buildEnvVars(mock(AbstractBuild.class), env );        
        assertEquals("The workfolder path was incorrect", "/this/is/a" + File.separator + "PATH", env.get(TeamFoundationServerScm.WORKFOLDER_ENV_STR));
    }
    
    /**
     * Workspace name must be less than 64 characters, cannot end with a space or period, and cannot contain any of the following characters: "/:<>|*?
     */
    @Test
    public void assertWorkspaceNameReplacesInvalidChars() {
        TeamFoundationServerScm scm = new TeamFoundationServerScm(null, null, ".", false, "A\"B/C:D<E>F|G*H?I", "user", "password");
        assertEquals("Workspace name contained invalid chars", "A_B_C_D_E_F_G_H_I", scm.getNormalizedWorkspaceName(null, null));
    }
    
    /**
     * Workspace name must be less than 64 characters, cannot end with a space or period, and cannot contain any of the following characters: "/:<>|*?
     */
    @Test
    public void assertWorkspaceNameReplacesEndingPeriod() {
        TeamFoundationServerScm scm = new TeamFoundationServerScm(null, null, ".", false, "Workspace.Name.", "user", "password");
        assertEquals("Workspace name ends with period", "Workspace.Name_", scm.getNormalizedWorkspaceName(null, null));
    }
    
    /**
     * Workspace name must be less than 64 characters, cannot end with a space or period, and cannot contain any of the following characters: "/:<>|*?
     */
    @Test
    public void assertWorkspaceNameReplacesEndingSpace() {
        TeamFoundationServerScm scm = new TeamFoundationServerScm(null, null, ".", false, "Workspace Name ", "user", "password");
        assertEquals("Workspace name ends with space", "Workspace Name_", scm.getNormalizedWorkspaceName(null, null));
    }    
}
