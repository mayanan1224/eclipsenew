package com.rj.bd.user.global;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rj.bd.util.DataUtil;
import com.rj.bd.util.DistUtil;
import com.rj.bd.util.HttpUtil;
import com.rj.bd.util.LogUtil;



/**
 * 权限检查
 */
public class UserRightChecker implements Filter {
	
	/** 免登录页面列表 */
	private ArrayList<String> FreePageList = new ArrayList<String>();
	
	/** 登录即可使用页面列表 */
	private ArrayList<String> LoginFreePageList = new ArrayList<String>();

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
		FreePageList.add("*.hf");
		FreePageList.add("index.jsp");
	
	}
		
	
	
	/**
	 * 检查用户是否具有页面访问权限。对于Session过期的，和 没有页面访问权限的，踢回登录页面。
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
	{
		
		// 1. 整理参数。得到 request、response、url 对象。
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String curURL = request.getServletPath().replace("//", "/");
		//LogUtil.log("检查页面进入权限："+curURL);
		
		// 2. 若属于不用检测的链接，直接通过检测，继续访问。
		if (FreePageList.contains(curURL)) {
			chain.doFilter(request, response);
			return;
		}
		
		// 3. 检查Session。防止未登录的访问。遇到Session中的UserAccount为空的，判断为未登录，或非法进入。
		String userAccount="";
		try {
			//若没有登录的，直接返回错误
			userAccount = DistUtil.getDistSession("UserAccount", request);
			
			if(userAccount==null || userAccount.equals("")){
				
				response.sendRedirect(HttpUtil.getRequestBaseURL(request)+"/global/error403.jsp");
				return; 
			}
		} catch (Exception e) { LogUtil.exception("", e, response); return; }

		
		// 4. 只需要登录，即可使用的功能（不需要做功能权限检查的）
		if (LoginFreePageList.contains(curURL)) {
			chain.doFilter(request, response);
			return;
		}
		
		
		// 5. 功能权限检查。通过URL检查该用户是否具有访问该页面的权限。若有权限，通过检测，放行，否则，提示非法操作。
		try {
			String userPageRight = DistUtil.getDistSession("UserPageRight", request);
			String[] rightURLArr = DataUtil.splitString(userPageRight, "[|@|]");
			
			//System.out.println("访问的地址---------》"+curURL);
			
			for(int i=0; i<rightURLArr.length; i++){
//				System.out.println("拥有的访问权限---------》"+rightURLArr[i]);
//				LogUtil.log("|"+rightURLArr[i]+"|  +  "+curURL+" = "+curURL.indexOf(rightURLArr[i]));
				if(rightURLArr[i].equals("")==false && curURL.indexOf(rightURLArr[i])==1){
					chain.doFilter(request, response);								//通过权限检测，继续访问
					return;
				}
			}
			
				response.sendRedirect(HttpUtil.getRequestBaseURL(request)+"/global/error403.jsp");
		   
				return; 
			
			
		} catch (Exception e) { LogUtil.exception("", e, response); return; }
		
	}
		
	@Override
	public void destroy() { }
}
