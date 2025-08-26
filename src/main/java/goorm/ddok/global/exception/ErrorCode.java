package goorm.ddok.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400 BAD REQUEST
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_PHONE_FORMAT(HttpStatus.BAD_REQUEST, "올바른 휴대전화 번호 형식이 아닙니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "이름은 한글 2자 이상만 입력 가능합니다."),
    PASSWORDS_DO_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST,"비밀번호는 8자 이상이어야 합니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 영어 대문자, 소문자, 숫자, 특수문자를 모두 포함해야 합니다."),
    PHONE_NUMBER_ALREADY_USED(HttpStatus.BAD_REQUEST, "기존 전화번호와 동일합니다."),
    INVALID_LEADER_POSITION(HttpStatus.BAD_REQUEST, "리더 포지션이 모집 포지션 목록에 존재하지 않습니다."),
    INVALID_CAPACITY(HttpStatus.BAD_REQUEST, "모집 인원은 1명 이상이어야 합니다."),
    INVALID_AGE_RANGE(HttpStatus.BAD_REQUEST, "연령대 범위가 올바르지 않습니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "위치 정보가 올바르지 않습니다."),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "시작일은 오늘 이후여야 합니다."),
    INVALID_BOUNDING_BOX(HttpStatus.BAD_REQUEST, "잘못된 지도 경계값입니다."),


    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token이 누락되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_NOT_VERIFIED_CODE_RESENT(HttpStatus.UNAUTHORIZED, "인증 코드가 만료되어 새로운 인증 이메일을 발송했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 일치하지 않습니다."),
    KAKAO_INVALID_CODE(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다. (유효하지 않은 코드)"),



    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),


    // 404 NOT FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 요청 기록이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    CAFE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카페입니다."),
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모집글을 찾을 수 없습니다."),




    // 409 CONFLICT
    DUPLICATE_NAME_AND_PHONE(HttpStatus.CONFLICT, "해당 이름과 연락처로 이미 가입된 회원이 있습니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증이 완료된 계정입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증이 완료된 요청입니다."),
    LEADER_NOT_FOUND(HttpStatus.CONFLICT, "리더 정보를 찾을 수 없습니다."),


    // 500 INTERNAL SERVER ERROR
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송 실패"),
    PROJECT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 저장 중 오류가 발생했습니다."),
    STUDY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "스터디 저장 중 오류가 발생했습니다."),
    BANNER_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배너 이미지 업로드에 실패했습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
