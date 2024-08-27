package com.example.dto;

import java.util.Date;

public class Clickstream {

	private String clickstreamId;

	private String session;

	private Date timestamp;

	private String pageUrl;

	public String getClickstreamId() {
		return clickstreamId;
	}

	public void setClickstreamId(String clickstreamId) {
		this.clickstreamId = clickstreamId;
	}

	public String getSession() {
		return session;
	}

	public void setSession(String session) {
		this.session = session;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

}
