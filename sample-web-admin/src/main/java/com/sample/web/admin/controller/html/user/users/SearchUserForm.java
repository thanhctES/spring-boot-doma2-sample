package com.sample.web.admin.controller.html.user.users;

import com.sample.domain.dto.common.Pageable;
import com.sample.web.base.controller.html.BaseSearchForm;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SearchUserForm extends BaseSearchForm implements Pageable {

    private static final long serialVersionUID = 4131372368553937515L;

    Integer id;

    String firstName;

    String lastName;

    String password;
}
