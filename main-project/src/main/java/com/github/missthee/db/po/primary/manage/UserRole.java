package com.github.missthee.db.po.primary.manage;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class UserRole implements Serializable {
    @Id
    private Long id;

    private Long userId;

    private Long roleId;

    private static final long serialVersionUID = 1L;

    public static final String ID = "id";

    public static final String DB_ID = "id";

    public static final String USER_ID = "userId";

    public static final String DB_USER_ID = "user_id";

    public static final String ROLE_ID = "roleId";

    public static final String DB_ROLE_ID = "role_id";
}