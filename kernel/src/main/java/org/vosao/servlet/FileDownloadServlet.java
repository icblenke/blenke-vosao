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

package org.vosao.servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vosao.business.decorators.TreeItemDecorator;
import org.vosao.common.VosaoContext;
import org.vosao.entity.FileEntity;
import org.vosao.entity.FolderEntity;
import org.vosao.i18n.Messages;
import org.vosao.utils.DateUtil;
import org.vosao.utils.FolderUtil;

/**
 * Servlet for download files from database.
 * 
 * @author Aleksandr Oleynik
 */
public class FileDownloadServlet extends BaseSpringServlet {
	
	private static final long CACHE_LIMIT = 1048576;
	
	private static final long serialVersionUID = 6098745782027999297L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String[] chain = FolderUtil.getPathChain(request.getPathInfo());
		if (chain.length == 0) {
			response.sendError(response.SC_NOT_FOUND, Messages.get(
					"file.not_specified"));
			return;
		}
		if (isInPublicCache(request.getPathInfo())) {
			if (sendFromCache(request, response)) {
				logger.info("from public cache " + request.getPathInfo());
				return;
			}
		}	
		String filename = chain[chain.length-1];		
		TreeItemDecorator<FolderEntity> tree = getBusiness().getFolderBusiness()
				.getTree();
		TreeItemDecorator<FolderEntity> folder = getBusiness().getFolderBusiness()
				.findFolderByPath(tree, FolderUtil.getFilePath(
						request.getPathInfo()));
		if (folder == null) {
			response.sendError(response.SC_NOT_FOUND, Messages.get(
					"folder.not_found", request.getPathInfo()));
			return;
		}
		if (isAccessDenied(folder.getEntity())) {
			response.sendError(response.SC_FORBIDDEN, Messages.get(
					"access_denied"));
			return;
		}
		if (isInCache(request.getPathInfo())) {
			if (VosaoContext.getInstance().getUser() == null) {
				getBusiness().getSystemService().getFileCache()
					.makePublic(request.getPathInfo());
			}
			if (sendFromCache(request, response)) {
				logger.info("from cache " + request.getPathInfo());
				return;
			}
		}	
		FileEntity file = getDao().getFileDao().getByName(
				folder.getEntity().getId(), filename);
		if (file != null) {
			byte[] content = getDao().getFileDao().getFileContent(file);
			if (file.getSize() < CACHE_LIMIT) {
				getBusiness().getSystemService().getFileCache()
					.put(request.getPathInfo(), file, content);
				if (VosaoContext.getInstance().getUser() == null) {
					getBusiness().getSystemService().getFileCache()
						.makePublic(request.getPathInfo());
				}
			}
			sendFile(file, content, request, response);
		}
		else {
			response.sendError(response.SC_NOT_FOUND, Messages.get(
					"file.not_found"));
	        logger.debug("not found file " + request.getPathInfo());
		}
	}
	
	private boolean isInCache(final String path) {
		return getBusiness().getSystemService().getFileCache().isInCache(path);
	}
	
	private boolean isInPublicCache(final String path) {
		return getBusiness().getSystemService().getFileCache().isInPublicCache(
				path);
	}

	/**
	 * Send file from file cache.
	 * @param request
	 * @param response
	 * @return true is file was successfully processed, false if not.
	 * @throws IOException
	 */
	private boolean sendFromCache(HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
		FileEntity file = getBusiness().getSystemService().getFileCache()
				.getFile(request.getPathInfo());
		if (file == null) {
			return false;
		}
		byte[] content = getBusiness().getSystemService().getFileCache()
				.getContent(request.getPathInfo());
		if (content == null) {
			return false;
		}
		sendFile(file, content, request, response);
		return true;
	}
	
	private void sendFile(final FileEntity file, byte[] content, 
			HttpServletRequest request,	HttpServletResponse response) 
			throws IOException {
		if(DateUtil.toHeaderString(file.getLastModifiedTime()).equals(
				request.getHeader("If-Modified-Since"))){
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		else {
			response.setHeader("Content-type", file.getMimeType());
			
			response.setHeader("Content-Length", String.valueOf(file.getSize()));
			response.setHeader("Last-Modified", 
					DateUtil.toHeaderString(file.getLastModifiedTime()));
			BufferedOutputStream output = new BufferedOutputStream(
					response.getOutputStream());
			output.write(content);
			output.flush();
			output.close();
		}
	}

	private boolean isAccessDenied(FolderEntity folder) {
		if (VosaoContext.getInstance().getUser() == null) {
			return getBusiness().getFolderPermissionBusiness()
					.getGuestPermission(folder).isDenied();
		}
		return getBusiness().getFolderPermissionBusiness()
				.getPermission(folder, VosaoContext.getInstance().getUser()).isDenied();
	}
	
	
}