package com.company.boarding.screen.user;

import com.company.boarding.entity.OnboardingStatus;
import com.company.boarding.entity.Step;
import com.company.boarding.entity.User;
import com.company.boarding.entity.UserStep;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.security.event.SingleUserPasswordChangeEvent;
import io.jmix.ui.Notifications;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.DataContext;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.navigation.Route;
import io.jmix.ui.screen.*;
import liquibase.pro.packaged.U;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

@UiController("User.edit")
@UiDescriptor("user-edit.xml")
@EditedEntityContainer("userDc")
@Route(value = "users/edit", parentPrefix = "users")
public class UserEdit extends StandardEditor<User> {

    @Autowired
    private CollectionPropertyContainer<UserStep> stepsDc;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private DataContext dataContext;

    @Autowired
    private EntityStates entityStates;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordField passwordField;

    @Autowired
    private TextField<String> usernameField;

    @Autowired
    private PasswordField confirmPasswordField;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    @Autowired
    private ComboBox<String> timeZoneField;

    @Autowired
    private UiComponents uiComponents;

    private boolean isNewEntity;

    @Subscribe
    public void onInit(InitEvent event) {
        timeZoneField.setOptionsList(Arrays.asList(TimeZone.getAvailableIDs()));
    }

    @Subscribe
    public void onInitEntity(InitEntityEvent<User> event) {
        usernameField.setEditable(true);
        passwordField.setVisible(true);
        confirmPasswordField.setVisible(true);
        isNewEntity = true;
//Внесли значение по умолчанию NOT_STARTED
        User user = event.getEntity();
        user.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            usernameField.focus();
        }
    }

    @Subscribe
    protected void onBeforeCommit(BeforeCommitChangesEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            if (!Objects.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("passwordsDoNotMatch"))
                        .show();
                event.preventCommit();
            }
            getEditedEntity().setPassword(passwordEncoder.encode(passwordField.getValue()));
        }
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onPostCommit(DataContext.PostCommitEvent event) {
        if (isNewEntity) {
            getApplicationContext().publishEvent(new SingleUserPasswordChangeEvent(getEditedEntity().getUsername(), passwordField.getValue()));
        }
    }

    @Subscribe("generateButton")
    public void onGenerateButtonClick(final Button.ClickEvent event) {
        User user = getEditedEntity();
        if (user.getJoiningDate() == null) {
            notifications.create()
                    .withCaption("Cannot generate steps for user without 'Joining date'")
                    .show();
            return;
        }
        List<Step> steps = dataManager.load(Step.class)
                .query("select s from Step s order by s.sortValue asc")
                .list();
        for (Step step: steps) {
            if (stepsDc.getItems().stream().noneMatch(userStep ->
                    userStep.getStep().equals(step))) {
                UserStep userStep = dataContext.create(UserStep.class);
                userStep.setUser(user);
                userStep.setStep(step);
                userStep.setDueDate(user.getJoiningDate().plusDays(step.getDuration()));
                userStep.setSortValue(step.getSortValue());
                stepsDc.getMutableItems().add(userStep);
            }
        }
    }

    @Install(to = "stepsTable.completed", subject = "columnGenerator")
    private Component stepsTableCompletedColumnGenerator(final UserStep userStep) {
        CheckBox checkBox = uiComponents.create(CheckBox.class);
        checkBox.setValue(userStep.getCompletedDate() != null);
        checkBox.addValueChangeListener(e -> {
            if (userStep.getCompletedDate() == null) {
                userStep.setCompletedDate(LocalDate.now());
            } else {
                userStep.setCompletedDate(null);
            }
        });
        return checkBox;
    }

    @Subscribe(id = "stepsDc", target = Target.DATA_CONTAINER)
    public void onStepsDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<UserStep> event) {

    }

    @Subscribe(id = "stepsDc", target = Target.DATA_CONTAINER)
    public void onStepsDcCollectionChange(final CollectionContainer.CollectionChangeEvent<UserStep> event) {

    }

    private void updateOnboardingStatus() {
        User user = getEditedEntity();

        long completedCount = user.getSteps() == null ? 0 :
                user.getSteps().stream()
                        .filter(us -> us.getCompletedDate() != null)
                        .count();
        if (completedCount == 0) {
            user.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
        } else if (completedCount == user.getSteps().size()) {
            user.setOnboardingStatus(OnboardingStatus.COMPLETED);
        } else {
            user.setOnboardingStatus(OnboardingStatus.IN_PROGRESS);
        }
    }
}