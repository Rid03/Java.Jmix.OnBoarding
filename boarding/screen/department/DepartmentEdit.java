package com.company.boarding.screen.department;

import io.jmix.ui.screen.*;
import com.company.boarding.entity.Department;

@UiController("Department.edit")
@UiDescriptor("department-edit.xml")
@EditedEntityContainer("departmentDc")
public class DepartmentEdit extends StandardEditor<Department> {
    @Subscribe
    public void onInitEntity(final InitEntityEvent<Department> event) {

    }
}