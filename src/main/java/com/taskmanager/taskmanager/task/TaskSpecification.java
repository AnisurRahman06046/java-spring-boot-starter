package com.taskmanager.taskmanager.task;

import org.springframework.data.jpa.domain.Specification;

public class TaskSpecification {
    // filter by status optional
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    // filter by user
    public static Specification<Task> hasUser(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user"), userId);
    }

    // filter by title or description containing keyword
    public static Specification<Task> titleOrDescriptionContains(String keyword){
        return (root,query,cb)->{
            if(keyword==null || keyword.isBlank()){
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
