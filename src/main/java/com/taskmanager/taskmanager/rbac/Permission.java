package com.taskmanager.taskmanager.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,unique=true)
    private String name; // ex: TASK:READ

    private String description;

    // all system permissions defined in one place
    public static final String TASK_READ="TASK:READ";
    public static final String TASK_CREATE    = "TASK:CREATE";
    public static final String TASK_UPDATE    = "TASK:UPDATE";
    public static final String TASK_DELETE    = "TASK:DELETE";
    public static final String USER_READ      = "USER:READ";
    public static final String USER_MANAGE    = "USER:MANAGE";
    public static final String ROLE_MANAGE    = "ROLE:MANAGE";

}
