package goorm.ddok.member.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import goorm.ddok.badge.domain.UserBadge;
import goorm.ddok.reputation.domain.UserReputation;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "nickname", unique = true, length = 12)
    private String nickname;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", unique = true, length = 11)
    private String phoneNumber;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 1024)
    private String profileImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "birth_date")
    @Past
    private LocalDate birthDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(length = 130)
    private String introduce;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @OneToOne(
            mappedBy = "user",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}
    )
    @ToString.Exclude
    @JsonIgnore
    private UserActivity activity;

    @OneToOne(
            mappedBy = "user",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}
    )
    @ToString.Exclude
    @JsonIgnore
    private UserLocation location;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private java.util.List<UserPosition> positions = new java.util.ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private java.util.List<UserTrait> traits = new java.util.ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private java.util.List<UserTechStack> techStacks = new java.util.ArrayList<>();

    @OneToOne(
            mappedBy = "user",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            fetch = FetchType.LAZY,
            optional = false
    )
    @ToString.Exclude
    @JsonIgnore
    private UserReputation reputation;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private List<UserBadge> badges = new ArrayList<>();



    public User(String username, String nickname, String email, String phoneNumber, String password, String profileImageUrl) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }

    public String getAgeGroup() {
        if (birthDate == null) {
            return null;
        }
        int age = java.time.Period.between(birthDate, LocalDate.now()).getYears();

        if (age < 10) {
            return "10세 미만";
        }

        int decade = (age / 10) * 10;
        return decade + "대";
    }

    public void addBadge(UserBadge badge) {
        badges.add(badge);
    }

    public void removeBadge(UserBadge badge) {
        badge.softDelete();
    }

}
