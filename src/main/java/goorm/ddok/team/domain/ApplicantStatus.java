package goorm.ddok.team.domain;

public enum ApplicantStatus {
    PENDING,
    APPROVED,
    REJECTED
    ;

    // Study용 매핑
    public static ApplicantStatus from(goorm.ddok.study.domain.ApplicationStatus status) {
        return ApplicantStatus.valueOf(status.name());
    }

    // Project용 매핑
    public static ApplicantStatus from(goorm.ddok.project.domain.ApplicationStatus status) {
        return ApplicantStatus.valueOf(status.name());
    }
}
