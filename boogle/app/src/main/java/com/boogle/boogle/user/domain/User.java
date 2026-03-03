package com.boogle.boogle.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_login_id",
                        columnNames = "login_id"
                )
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String role; // admin

    // 실패 횟수 저장한은 필드
    private int failCount = 0;

    // 실패 횟수 증가 및 초기화
    public void increaseFailCount() {
        failCount++;
    }

    public void resetFailCount() {
        failCount = 0;
    }

}
