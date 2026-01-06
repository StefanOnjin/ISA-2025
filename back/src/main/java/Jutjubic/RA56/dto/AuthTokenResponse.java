package Jutjubic.RA56.dto;

public class AuthTokenResponse {
	private String accessToken;
	private int expiresIn;

	public AuthTokenResponse() {
	}

	public AuthTokenResponse(String accessToken, int expiresIn) {
		this.accessToken = accessToken;
		this.expiresIn = expiresIn;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(int expiresIn) {
		this.expiresIn = expiresIn;
	}
}
