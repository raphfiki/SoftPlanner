package com.raphjava.softplanner.domain;

import com.raphjava.softplanner.components.AbFactoryBean;
import com.raphjava.softplanner.components.Action;
import com.raphjava.softplanner.components.ComponentBase;
import com.raphjava.softplanner.components.ConsoleInput;
import com.raphjava.softplanner.data.models.Component;
import com.raphjava.softplanner.data.models.Project;
import com.raphjava.softplanner.services.ConsoleOutputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.util.Queue;

import static com.raphjava.softplanner.annotations.Scope.Singleton;

public class ProjectAddition extends ComponentBase
{


    private ProjectAddition(Builder builder)
    {
        super(builder.baseBuilder);
        outputService = builder.outputService;
        inputService = builder.inputService;
        projectDataParser = builder.projectDataParser;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final String FACTORY = "projectAdditionFactory";

    private String projectDataTemplate = "[Project name-Soft Planner], [Description-Helps in the planning the development of an app]";

    private ConsoleInput inputService;

    private ProjectDataParser projectDataParser;

    public void addProject(Queue<String> args)
    {
        projectDataParser.processData(asExp(args).selectToObject(new StringBuilder(), StringBuilder::append).toString())
                .ifPresent(this::addNewProject);

    }

    public void startAsConsole()
    {
        show(String.format("Enter new project details in the following format: %s", projectDataTemplate));
        inputService.getInput().flatMap(projectDataParser::processData).ifPresent(this::addNewProject);

    }

    private void addNewProject(Project project)
    {

        show(String.format("Adding new project with the following details: Project name: %s. " +
                "Project description: %s. Please wait...", project.getName(), project.getRoot().getDescription()));

        dataService.write(w -> w
//            .add(project.getRoot().getSubComponentDetail())
            .add(project.getRoot()/*, e -> e.include(Component.SUB_COMPONENT_DETAIL)*/)
            .add(project, e -> e.include(Project.ROOT))
            .commit()
            .onSuccess(() ->
            {
                dataService.read(r -> r
                    .get(Project.class, e -> e.equation().path("id").constant(project.getId()))
                        .eagerLoad(l -> l.include(path(Project.ROOT, Component.SUB_COMPONENT_DETAIL)))
                        .onSuccess(ps ->
                        {
                            String failureMessage = "Adding of project failed.";
                            if(ps.isEmpty()) show(failureMessage);
                            else
                            {
                                Project p = ps.iterator().next();
                                if(p == null) show(failureMessage);
                                else if(p.getId() != project.getId()) show(failureMessage +
                                        " the repository copy is not equal to the domain copy.");
                            }

                        }));
                show("Adding of project successful.");
            })
            .onFailure(() -> show("Adding of project failed.")));

        show("Adding new project complete.");


    }


    private void initialize()
    {
        Action anp = actionFactory.createProduct();
        anp.setCommandDescription("Add New Project");
        anp.setAction(this::startAsConsole);
        getCommands().add(anp);


    }


    @Lazy
    @org.springframework.stereotype.Component(FACTORY)
    @Scope(Singleton)
    public static final class Builder extends AbFactoryBean<ProjectAddition>
    {
        private ComponentBase.Builder baseBuilder;

        private ConsoleInput inputService;
        private ProjectDataParser projectDataParser;
        private ConsoleOutputService outputService;

        private Builder()
        {
            super(ProjectAddition.class);
        }

        @Autowired
        public Builder baseBuilder(ComponentBase.Builder baseBuilder)
        {
            this.baseBuilder = baseBuilder;
            return this;
        }

        @Autowired
        public Builder inputService(ConsoleInput inputService)
        {
            this.inputService = inputService;
            return this;
        }

        @Autowired
        public Builder projectDataParser(ProjectDataParser projectDataParser)
        {
            this.projectDataParser = projectDataParser;
            return this;
        }

        public ProjectAddition build()
        {
            ProjectAddition pa = new ProjectAddition(this);
            pa.initialize();
            return pa;

        }

        @Autowired
        public Builder outputService(ConsoleOutputService outputService)
        {
            this.outputService = outputService;
            return this;
        }
    }
}
