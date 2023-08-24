package com.company.boarding.screen.step;

import io.jmix.ui.screen.*;
import com.company.boarding.entity.Step;

@UiController("Step.browse")
@UiDescriptor("step-browse.xml")
@LookupComponent("stepsTable")
public class StepBrowse extends StandardLookup<Step> {
}