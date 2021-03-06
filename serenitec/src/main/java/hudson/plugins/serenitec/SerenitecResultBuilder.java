/**
 * Hudson Serenitec plugin
 *
 * @author Georges Bossert <gbossert@gmail.com>
 * @version $Revision: 1.5 $
 * @since $Date: 2008/07/24 09:44:13 ${date}
 * @copyright Université de Rennes 1
 */
package hudson.plugins.serenitec;

import hudson.plugins.serenitec.util.Project;
import hudson.model.AbstractBuild;



/**
 * Creates a new warnings result based on the values of a previous build and the
 * current project.
 *
 * @author Ulli Hafner
 */
public class SerenitecResultBuilder
{

        
    /**
     * Creates a result that persists the serenitec information for the
     * specified build.
     *
     * @param build
     *            the build to create the action for
     * @param project
     *            the project containing the annotations
     * @return the result action
     */
    public SerenitecResult build(final AbstractBuild < ?, ? > build,
            final Project project)
    {
        System.out.println("Executing Serenitec Result.");
        Object previous = build.getPreviousBuild();
        System.out.println("build.getPreviousBuild");
        while (previous != null && previous instanceof AbstractBuild< ?, ? >)
        {
            AbstractBuild <?, ?> previousBuild = (AbstractBuild < ?, ? >) previous;
            SerenitecResultAction previousAction = previousBuild.getAction(SerenitecResultAction.class);
            
            if (previousAction != null)
            {
                System.out.println("previousAction != null");
                return new SerenitecResult(build, project, previousAction.getResult());
            }
            previous = previousBuild.getPreviousBuild();
            
        }
        System.out.println("End of Executing Serenitec Result.");
        System.out.println("returning a Serenitec Result without trend");
        return new SerenitecResult(build, project);

    }
}

