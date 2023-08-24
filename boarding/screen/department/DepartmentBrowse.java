package com.company.boarding.screen.department;

import io.jmix.ui.screen.*;
import com.company.boarding.entity.Department;

@UiController("Department.browse")
@UiDescriptor("department-browse.xml")
@LookupComponent("departmentsTable")
public class DepartmentBrowse extends StandardLookup<Department> {
}