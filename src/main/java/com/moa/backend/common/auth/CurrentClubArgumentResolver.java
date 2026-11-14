package com.moa.backend.common.auth;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentClubArgumentResolver implements HandlerMethodArgumentResolver {

	public static final String INVITE_CODE_HEADER = "X-Invite-Code";

	private final ClubRepository clubRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentClub.class)
				&& Club.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Club resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String inviteCode = webRequest.getHeader(INVITE_CODE_HEADER);
		if (!StringUtils.hasText(inviteCode)) {
			throw new ApiException(ErrorCode.INVITE_CODE_REQUIRED);
		}
		return clubRepository.findByInviteCode(inviteCode.trim())
				.orElseThrow(() -> new ApiException(ErrorCode.INVITE_CODE_INVALID));
	}
}
