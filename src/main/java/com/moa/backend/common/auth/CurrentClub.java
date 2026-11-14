package com.moa.backend.common.auth;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 {@code X-Invite-Code} 헤더의 초대 코드로 찾은 {@code Club}이 주입된다.
 * 헤더가 없거나 일치하는 동아리가 없으면 401로 응답한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface CurrentClub {
}
