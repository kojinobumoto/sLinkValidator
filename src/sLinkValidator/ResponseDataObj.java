// Copyright 2015 Koji Nobumoto
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sLinkValidator;

public class ResponseDataObj {
	private final String pageTitle;
	private final String respMsg;
	private final int respCode;
	private final String redirectUrl;
	private final int respCodeRedirectTo;
	private final String respMsgRedirectTo;
	
	public ResponseDataObj(String pageTitle, String responseMessage, int responseCode, String redirectTo, int responseCodeRedirectTo, String responseMessageRedirectTo) {
		this.pageTitle = pageTitle;
		this.respMsg = responseMessage;
		this.respCode = responseCode;
		this.redirectUrl = redirectTo;
		this.respCodeRedirectTo = responseCodeRedirectTo;
		this.respMsgRedirectTo = responseMessageRedirectTo;
	}
	
	public String getPageTitle() {
		return pageTitle;
	}
	public String getRespMsg() {
		return respMsg;
	}
	public int getRespCode() {
		return respCode;
	}
	public String getRedirectUrl() {
		return redirectUrl;
	}
	public int getRespCodeRedirectTo() {
		return respCodeRedirectTo;
	}
	public String getRespMsgRedirectTo() {
		return respMsgRedirectTo;
	}
}
