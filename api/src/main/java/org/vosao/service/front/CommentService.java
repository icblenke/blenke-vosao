/**
 * Vosao CMS. Simple CMS for Google App Engine.
 * Copyright (C) 2009 Vosao development team
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * email: vosao.dev@gmail.com
 */

package org.vosao.service.front;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.vosao.service.AbstractService;
import org.vosao.service.ServiceResponse;
import org.vosao.service.vo.CommentVO;


public interface CommentService extends AbstractService {
	
	List<CommentVO> getByPage(final String pageUrl);

	/**
	 * Add comment to page. Protected by reCaptcha service.
	 * @param name - user name
	 * @param params - form parameters
	 * @param challenge - recaptcha challenge
	 * @param response - recaptcha response
	 * @return - service response.
	 */
	ServiceResponse addComment(final String name, 
			final String comment, 
			final String pageUrl,
			final String challenge, 
			final String response, 
			HttpServletRequest request);
	
}
