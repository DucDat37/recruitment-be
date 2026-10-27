package com.example.recruitment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	/**
	 * Thêm nút Authorize vào Swagger UI. Dán access token (không cần chữ "Bearer ")
	 * sau khi đăng nhập, các API bảo vệ sẽ tự gửi kèm token.
	 */
	@Bean
	public OpenAPI recruitmentOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Recruitment API")
						.version("v1")
						.description("API hệ thống tuyển dụng tích hợp AI phân tích CV và gợi ý việc làm"))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
				.components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
						.name(BEARER_SCHEME)
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}
