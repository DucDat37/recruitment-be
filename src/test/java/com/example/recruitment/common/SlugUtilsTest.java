package com.example.recruitment.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

	@Test
	void removesVietnameseDiacritics() {
		assertThat(SlugUtils.toSlug("Công nghệ thông tin", 150)).isEqualTo("cong-nghe-thong-tin");
		assertThat(SlugUtils.toSlug("Điện - Điện tử", 150)).isEqualTo("dien-dien-tu");
		assertThat(SlugUtils.toSlug("Kế toán / Kiểm toán", 150)).isEqualTo("ke-toan-kiem-toan");
	}

	@Test
	void collapsesSymbolsAndTrimsHyphens() {
		assertThat(SlugUtils.toSlug("  --C++ & .NET!!  ", 150)).isEqualTo("c-net");
	}

	@Test
	void returnsEmptyWhenNoLettersOrDigits() {
		assertThat(SlugUtils.toSlug("!!! ---", 150)).isEmpty();
	}

	@Test
	void cutsToMaxLengthWithoutTrailingHyphen() {
		assertThat(SlugUtils.toSlug("abc def", 4)).isEqualTo("abc");
	}
}
