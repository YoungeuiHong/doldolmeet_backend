package com.doldolmeet.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    NOT_ADMIN(HttpStatus.BAD_REQUEST, "관리자 회원이 아닙니다."),
    NOT_USER(HttpStatus.BAD_REQUEST, "가입된 아이디가 아닙니다."),
    ALREADY_JOIN_USER(HttpStatus.BAD_REQUEST, "이미 가입한 회원입니다. 로그인해주세요"),
    DUPLICATE_IDENTIFIER(HttpStatus.BAD_REQUEST, "중복된 이메일 입니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "중복된 닉네임 입니다."),
    DUPLICATE_GITHUB_ID(HttpStatus.BAD_REQUEST, "중복된 깃허브 아이디 입니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "잘못된 아이디 입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "잘못된 비밀번호 입니다."),
    PLZ_INPUT_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호를 입력해주세요"),
    PLZ_INPUT_CONTENT(HttpStatus.BAD_REQUEST, "내용을 입력해주세요"),
    EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "이메일을 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    MOGAKKO_IS_FULL(HttpStatus.BAD_REQUEST, "모각코 인원이 마감되었습니다."),
    ALREADY_OUT_MEMBER(HttpStatus.BAD_REQUEST, "이미 방에서 나간 유저 입니다."),
    ALREADY_ENTER_MEMBER(HttpStatus.BAD_REQUEST, "이미 입장한 유저 입니다."),
    NOT_MOGAKKO_MEMBER(HttpStatus.BAD_REQUEST, "방에 있는 멤버가 아닙니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MOGAKKO_NOT_FOUND(HttpStatus.NOT_FOUND, "모각코 방이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다."),
    CANNOT_FOUND_FRIEND(HttpStatus.NOT_FOUND, "친구를 찾을 수 없습니다."),
    NOT_SUPPORTED_SOCIALTYPE(HttpStatus.NOT_FOUND, "지원하지 않는 소셜로그인 입니다."),
    NOTIFICATION_SENDING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 발송에 실패했습니다"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 메세지가 존재하지 않습니다."),
    INVALID_FRIEND_CODE(HttpStatus.BAD_REQUEST, "친구 코드를 정확하게 입력해주세요."),
    CANNOT_REQUEST(HttpStatus.BAD_REQUEST, "자신에게 요청을 보낼 수 없습니다."),
    PLZ_INPUT(HttpStatus.BAD_REQUEST, "입력해주세요."),
    PLZ_INPUT_REASON_OF_REPORT(HttpStatus.BAD_REQUEST, "신고 이유를 입력해주세요."),
    GITHUB_TOKEN_REQUEST_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 에서 엑세스 토큰을 가져오지 못했습니다."),
    FAILED_TO_GET_USERINFO(HttpStatus.BAD_REQUEST, "GitHub로 부터 정보를 받아오지 못했습니다."),
    USER_MISMATCH_ERROR(HttpStatus.BAD_REQUEST, "사용자와 해당 메세지의 발신자 혹은 수신자가 일치하지 않습니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팀을 찾을 수 없습니다."),
    IDOL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 아이돌을 찾을 수 없습니다."),
    FANMEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팬미팅을  찾을 수 없습니다."),
    FANMEETING_NOT_APPLIED(HttpStatus.NOT_FOUND, "팬미팅이 승인되지 않았습니다."),
    WAITROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 대기방을 찾을 수 없습니다."),
    //인증
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),

    //serverError
    INTERNAL_SERER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"),
    WAITROOM_FAN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 대기방에 팬이 없습니다."),
    FAN_TO_FANMEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팬미팅에 팬이 없습니다."),
    WAITROOMFAN_NOT_FOUND(HttpStatus.NOT_FOUND, "팬미팅 입장 안했습니다."),
    IDOL_NOT_IN_FANMEETING(HttpStatus.NOT_FOUND, "해당 아이돌은 팬미팅에 참여할 수 없습니다."),
    WAITROOMFAN_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "팬이 이미 대기방에서 기다리고 있습니다."),
    TEAM_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 팀입니다."),
    TELE_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 텔레그램 방을 찾을 수 없습니다."),
    TELE_ROOMFAN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 화상방에 TeleRoomFan이 없습니다."),
    FAN_NOT_IN_ROOM(HttpStatus.NOT_FOUND, "해당 팬미팅에 팬이 없습니다."),
    TELEROOMFAN_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "해당 화상방에 이미 같은 팬이 존재합니다."),
    UNKNOWN_TYPE(HttpStatus.BAD_REQUEST, "알 수 없는 타입입니다."),
    NOT_FOUND_FANTOFANMEETING(HttpStatus.NOT_FOUND, "해당 팬미팅을 신청하지 않은 팬입니다."),
    NOT_FOUND_FANMEETING_ROOM_ORDER(HttpStatus.NOT_FOUND, "해당 세션ID를 가지는 방이 존재하지 않습니다."),
    INVALID_IDOLROOM_STATE(HttpStatus.BAD_REQUEST, "아이돌 방의 상태가 유효하지 않습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 세션을 찾을 수 없습니다."),
    INVALID_FANMEETING_END(HttpStatus.BAD_REQUEST, "대기방의 다음 방이 없습니다."),
    SSE_NOT_SENT_FIRST_IDOL_WAIT_ROOM(HttpStatus.INTERNAL_SERVER_ERROR, "첫번째 아이돌 대기방으로 이동하는 SSE 이벤트를 보내지 못했습니다."),
    SLEEP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Thread.sleep() 실패"),
    INVALID_USER_TYPE(HttpStatus.BAD_REQUEST, "유저 타입이 유효하지 않습니다."),
    EMITTER_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SSE 이벤트를 보내지 못했습니다."),
    THREAD_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "쓰레드 인터럽트되어 SLEEP 중단"),
    FANMEETING_ROOMS_NOT_CREATED(HttpStatus.NOT_FOUND, "팬미팅 방이 생성되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String data;
}
