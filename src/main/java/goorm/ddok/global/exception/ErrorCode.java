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
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 영어 대문자, 소문자, 숫자, 특수문자를 모두 포함 8자 이상이어야 합니다."),
    PHONE_NUMBER_ALREADY_USED(HttpStatus.BAD_REQUEST, "기존 전화번호와 동일합니다."),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "잘못된 검색어입니다."),
    INVALID_ROOM_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 roomID 입니다."),
    MISSING_KEYWORD(HttpStatus.BAD_REQUEST, "keyword는 필수입니다."),
    INVALID_KEYWORD_LENGTH(HttpStatus.BAD_REQUEST, "keyword는 1~50자여야 합니다."),
    INVALID_LEADER_POSITION(HttpStatus.BAD_REQUEST, "리더 포지션이 모집 포지션 목록에 존재하지 않습니다."),
    INVALID_CAPACITY(HttpStatus.BAD_REQUEST, "모집 인원은 1명 이상이어야 합니다."),
    INVALID_AGE_RANGE(HttpStatus.BAD_REQUEST, "연령대 범위가 올바르지 않습니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "위치 정보가 올바르지 않습니다."),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "시작일은 오늘 이후여야 합니다."),
    INVALID_BOUNDING_BOX(HttpStatus.BAD_REQUEST, "잘못된 지도 경계값입니다."),
    USER_POSITION_NOT_FOUND(HttpStatus.BAD_REQUEST, "사용자의 메인 포지션이 설정되지 않았습니다."),
    INVALID_CONFIRM_TEXT(HttpStatus.BAD_REQUEST, "확인 문구가 올바르지 않습니다."),
    INVALID_POSITIONS(HttpStatus.BAD_REQUEST,"모집 포지션은 최소 1개 이상이어야 합니다."),
    INVALID_AGE_BUCKET(HttpStatus.BAD_REQUEST, "연령은 10단위(예: 20, 30, 40)만 허용합니다."),
    INVALID_CAPACITY_POSITIONS(HttpStatus.BAD_REQUEST,"포지션의 개수는 모집인원을 넘을 수 없습니다."),
    POSITION_IN_USE(HttpStatus.BAD_REQUEST, "참여자/지원 이력 때문에 삭제할 수 없는 포지션입니다."),
    INVALID_STUDY_TYPE(HttpStatus.BAD_REQUEST, "스터디 유형은 필수이며 올바른 값이어야 합니다."),
    INVALID_MAP_BOUNDS(HttpStatus.BAD_REQUEST, "잘못된 지도 경계값입니다."),
    REQUIRED_PARAMETER_MISSING(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀 정보를 찾을 수 없습니다."),
    PROFILE_MAIN_POSITION_REQUIRED(HttpStatus.BAD_REQUEST, "메인 포지션은 필수입니다."),
    PROFILE_SECONDARY_POSITION_TOO_MANY(HttpStatus.BAD_REQUEST, "서브 포지션은 최대 2개까지 설정할 수 있습니다."),
    PROFILE_POSITION_DUPLICATED(HttpStatus.BAD_REQUEST, "메인/서브 포지션에 중복 값이 포함되어 있습니다."),
    ACTIVE_HOURS_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "활동 시간 형식이 올바르지 않습니다.(00~24)"),
    ACTIVE_HOURS_RANGE_INVALID(HttpStatus.BAD_REQUEST, "활동 종료 시간은 시작 시간보다 빠를 수 없습니다."),
    TECH_STACK_NAME_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 기술 스택 값이 포함되어 있습니다."),
    TRAIT_NAME_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 성향 값이 포함되어 있습니다."),
    NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다."),
    PHONE_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "휴대폰 번호 형식이 올바르지 않습니다."),
    PROFILE_IMAGE_URL_INVALID(HttpStatus.BAD_REQUEST, "프로필 이미지 URL이 유효하지 않습니다."),
    PASSWORD_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "비밀번호는 이 API에서 변경할 수 없습니다."),
    PORTFOLIO_TITLE_INVALID(HttpStatus.BAD_REQUEST, "포트폴리오 링크 제목은 1~15자여야 합니다."),
    PORTFOLIO_URL_REQUIRED(HttpStatus.BAD_REQUEST, "포트폴리오 링크 주소는 필수입니다."),
    PORTFOLIO_URL_INVALID(HttpStatus.BAD_REQUEST, "유효한 URL 형식이 아닙니다. http/https만 허용합니다."),
    PORTFOLIO_TOO_MANY(HttpStatus.BAD_REQUEST, "포트폴리오는 최대 20개까지 등록할 수 있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "부적절한 파일 타입입니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "부적절한 파일입니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기는 5MB를 넘을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "파일 업로드에 실패하였습니다."),
    INVALID_TEAM_TYPE(HttpStatus.BAD_REQUEST,"잘못된 팀 타입입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    NOT_SUPPORT_CATEGORY(HttpStatus.BAD_REQUEST, "지원하지 않는 카테고리입니다."),
    EVALUATION_CLOSED(HttpStatus.BAD_REQUEST, "평가 기간이 종료되어 저장할 수 없습니다."),
    EVALUATION_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 평가할 수 없습니다."),
    EVALUATION_ITEM_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "평가 점수가 허용 범위를 벗어났습니다."),
    EVALUATION_ALREADY_SUBMITTED(HttpStatus.BAD_REQUEST, "이미 제출한 평가는 수정할 수 없습니다."),
    EVALUATION_TARGET_NOT_MEMBER(HttpStatus.BAD_REQUEST, "평가 대상이 팀원이 아닙니다."),
    INVALID_APPLICATION_STATUS(HttpStatus.BAD_REQUEST, "잘못된 신청 상태 값입니다."),
    CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "팀 정원을 초과할 수 없습니다."),
    PROJECT_POSITION_CLOSED(HttpStatus.BAD_REQUEST, "해당 포지션은 마감되었습니다."),
    APPLICATION_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "이미 승인된 신청은 취소할 수 없습니다."),
    ALREADY_EXPELLED(HttpStatus.BAD_REQUEST, "이미 추방되었거나 탈퇴한 팀원입니다."),
    LEADER_CANNOT_BE_EXPELLED(HttpStatus.BAD_REQUEST, "리더는 추방할 수 없습니다."),
    LEADER_CANNOT_WITHDRAW(HttpStatus.BAD_REQUEST, "리더는 하차할 수 없습니다."),
    INVALID_REVIEW_TAG(HttpStatus.BAD_REQUEST, "잘못된 리뷰 태그가 포함되어 있습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST,"평점이 올바르지 않습니다."),
    CHAT_MESSAGE_INVALID(HttpStatus.BAD_REQUEST, "메세지 내용이 없습니다."),
    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, "현재 모집 중이 아닙니다."),
    INVALID_CHAT_RANGE(HttpStatus.BAD_REQUEST,"채팅 범위가 적절하지 않습니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token이 누락되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_NOT_VERIFIED_CODE_RESENT(HttpStatus.UNAUTHORIZED, "인증 코드가 만료되어 새로운 인증 이메일을 발송했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 일치하지 않습니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다"),
    POSITION_REQUIRED(HttpStatus.BAD_REQUEST, "지원 포지션을 선택해야 합니다."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    REAUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "재인증이 필요합니다."),
    INVALID_REAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 재인증 토큰입니다."),


    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_CHAT_MEMBER(HttpStatus.FORBIDDEN, "채팅방에 참여하지 않은 사용자입니다."),
    FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "리더는 참여 신청을 할 수 없습니다."),
    REAUTH_USER_MISMATCH(HttpStatus.FORBIDDEN, "재인증 사용자 정보가 일치하지 않습니다."),
    FORBIDDEN_TEAM_ACCESS(HttpStatus.FORBIDDEN, "팀에 소속되지 않은 사용자는 접근할 수 없습니다."),
    FORBIDDEN_LEADER_ONLY(HttpStatus.FORBIDDEN,"해당 작업은 팀 리더만 가능합니다."),


    // 404 NOT FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 요청 기록이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    CAFE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카페입니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모집글을 찾을 수 없습니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 프로젝트입니다."),
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 포지션을 찾을 수 없습니다."),
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스터디입니다."),
    EVALUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "평가 라운드를 찾을 수 없습니다."),
    REPUTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 온도 정보를 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 신청 내역을 찾을 수 없습니다."),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "팀원을 찾을 수 없습니다."),
    RANKING_NOT_READY(HttpStatus.NOT_FOUND, "아직 랭킹 데이터가 준비되지 않았습니다."),


    // 409 CONFLICT
    DUPLICATE_NAME_AND_PHONE(HttpStatus.CONFLICT, "해당 이름과 연락처로 이미 가입된 회원이 있습니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증이 완료된 계정입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증이 완료된 요청입니다."),
    LEADER_NOT_FOUND(HttpStatus.CONFLICT, "리더 정보를 찾을 수 없습니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 해당 프로젝트에 다른 포지션으로 지원하였습니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 채팅방입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다."),
    ALREADY_PROCESSED_APPLICATION(HttpStatus.CONFLICT, "이미 처리된 신청입니다."),
    ALREADY_CLOSED(HttpStatus.CONFLICT,"이미 종료된 프로젝트/스터디입니다."),

    // 429 TOO MANY REQUESTS
    KAKAO_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "카카오 토큰 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),

    // 500 INTERNAL SERVER ERROR
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송 실패"),
    PROJECT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 저장 중 오류가 발생했습니다."),
    STUDY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "스터디 저장 중 오류가 발생했습니다."),
    BANNER_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배너 이미지 업로드에 실패했습니다."),
    PROJECT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 삭제 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
