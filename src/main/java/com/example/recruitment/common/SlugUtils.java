package com.example.recruitment.common;

import java.text.Normalizer;
import java.util.Locale;

/** Builds URL-friendly slugs from display names, including Vietnamese text. */
public final class SlugUtils {

	private SlugUtils() {
	}

	/**
	 * Converts a name to a lowercase ASCII slug, e.g. "Công nghệ thông tin" becomes "cong-nghe-thong-tin".
	 * Returns an empty string when the name has no letters or digits.
	 *
	 * @param maxLength maximum slug length; the slug is cut on this limit without a trailing hyphen
	 */
	public static String toSlug(String name, int maxLength) {
		String ascii = Normalizer.normalize(name, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				// "đ" is a separate letter, not "d" plus a combining mark, so NFD does not strip it
				.replace('đ', 'd')
				.replace('Đ', 'D');
		String slug = ascii.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
		if (slug.length() > maxLength) {
			slug = slug.substring(0, maxLength).replaceAll("-+$", "");
		}
		return slug;
	}
}
