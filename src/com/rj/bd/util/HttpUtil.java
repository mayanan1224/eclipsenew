package com.rj.bd.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class HttpUtil {

	
	
	/**
	 * 发起网页调用，获取网页调用返回值。调用可以是Post方式也可以是Get方式。当为Get方式时，第二个参数postData设置为null。
	 * 若发生调用失败，返回null ! eg:
	 * System.out.println(HttpUtil.callWebPage("http://www.baidu.com", null));
	 * System.out.println(HttpUtil.callWebPage(
	 * "http://market.aliyun.com/image/?spm=5176.383338.201.27.US4LIi", null));
	 * System.out.println(HttpUtil.callWebPage(
	 * "http://127.0.0.1:8080/demobo/global.Login.doLogin.hf?userAccount=admin&type=11"
	 * , "userPassword=haha&userAge=12"));
	 * 
	 * @param urlStr  HTTP Url 带参数。如：http://www.xxx.com/a.jsp?a=1
	 * @param postData  Post数据，形如：userPassword=haha&userAge=12
	 * @return String WEB页面调用后返回的HTML代码。调用失败，返回null。
	 */
	public static String callWebPage(String urlStr, String postData) {
		return callWebPage(urlStr, postData, "UTF-8");
	}
	
	
	/**
	 * 发起网页调用，获取网页调用返回值。调用可以是Post方式也可以是Get方式。当为Get方式时，第二个参数postData设置为null。
	 * 若发生调用失败，返回null ! eg:
	 * System.out.println(HttpUtil.callWebPage("http://www.baidu.com", null, "GBK"));
	 * 
	 * @param urlStr  HTTP Url 带参数。如：http://www.xxx.com/a.jsp?a=1
	 * @param postData Post数据，形如：userPassword=haha&userAge=12
	 * @param encoding 强制输入输出流的编码格式，为空或NULL的时候取默认值
	 * @return String WEB页面调用后返回的HTML代码。调用失败，返回null。
	 */
	public static String callWebPage(String urlStr, String postData, String encoding) {

		String rStr = null;
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			// 打开和URL之间的连接
			URLConnection conn = (new URL(urlStr)).openConnection();
			StringBuffer sb = new StringBuffer();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			if (postData != null) {
				conn.setDoOutput(true);
				conn.setDoInput(true);
			}
			// 建立实际的连接
			conn.connect();
			// Post数据（如果非空的话， 获取URLConnection对象对应的输出流并输出参数）
			if (postData != null) {
				if(encoding==null || "".equals(encoding)){
					out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()), true);
				}else{
					out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(),  encoding), true);
				}
				out.print(postData);
				out.flush();
			}
			// 获取所有响应头字段
			// Map<String, List<String>> map = connection.getHeaderFields();
			// for (String key : map.keySet()) { System.out.println(key +
			// " <== " + map.get(key)); }
			// 定义 BufferedReader输入流来读取URL的响应
			if(encoding==null || "".equals(encoding)){
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			}else{
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(),  encoding));
			}
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			rStr = sb.toString();
		} catch (Exception e) {
			LogUtil.exception(e);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (Exception ee) {
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception ee) {
			}
		}
		return rStr;
	}

	
	
	
	/**
	 * 获得Http方式Post或Get过来的所有参数的列表，以HashTable形式返回。即包括URL中的参数部分，也包括
	 * application/x-www-form-urlencoded 和 multipart/form-data 两种
	 * 编码方式提交的Form表单。其中后者主要用作提交文件。需要注意的是：
	 * 1）提交过来的参数中，有上传文件的，上传文件自动存放在 [网站根目录]/asset/upload/ 之下。
	 * 文件的命名方式 为 “UUID--文件原始名称”以防止文件重名。如：“.../asset/upload/747307ac-ee25-4048-900e-bf2e3d264aed--btn212-76.png”
	 * 2）对于提交过来的参数名称有重复的，仅第一个获得解析的参数的值有效，后续的均被忽略！
	 * 
	 * @param request 页面的Request对象
	 * @param onlyMultiFlag 为True，表示只返回 “multipart/form-data”类型的表单参数数据。为False，则标识返回 URL 和两种Form类型的参数的列表。
	 * @return 所有参数的键值对，以Hashtable形式返回
	 */
	public static Hashtable<String, String> getParameters(HttpServletRequest request, boolean onlyMultiFlag) {

		Hashtable<String, String> rObj = new Hashtable<String, String>();

		// 将常规的Request.getParameter()的参数和取值亦放入。即Get中URL后面的参数，和 Post的Form
		// Type为：application/x-www-form-urlencoded 的
		if (onlyMultiFlag == false) {
			Enumeration<?> pNames = request.getParameterNames();
			while (pNames.hasMoreElements()) {
				String itemName = (String) pNames.nextElement();
				String itemValue = request.getParameter(itemName);
				if (rObj.containsKey(itemName) == false) {
					rObj.put(itemName, itemValue);
				} else {
					LogUtil.exception("[HttpUtil] 参数 '" + itemName
							+ "' 重复（已经存在）。" + itemName + "=" + itemValue
							+ " 被跳过！");
				}
			}
		}

		// 将Post的Form Type为 multipart/form-data 的参数和取值 放入返回对象
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setProgressListener(new HttpUtil().new GetParametersProgressListener(request));
		request.getSession().setAttribute("HttpUtil.GetParameters.BeginTime", new Date());			//记录开始时间，以便后续计算剩余时间使用
		try {
			Iterator<?> itr = null;
			try{ itr = upload.parseRequest(request).iterator(); }catch(Exception ee){}
			if(itr!=null){
				while (itr.hasNext()) {
					FileItem item = (FileItem) itr.next();
					String itemName = item.getFieldName();
					// 检查itemName是否已经存在，若已存在，则跳过！
					if (rObj.containsKey(itemName)) {
						LogUtil.exception("[HttpUtil] 参数 '" + itemName
								+ "' 重复（已经存在）。" + itemName + "=" + item.getString()
								+ " 被跳过！");
						continue;
					}
					// 检查本字段是文件二进制内容字段还是普通Form元素字段
					if (item.isFormField()) {
						// 普通FormField
						rObj.put(itemName, item.getString());
					} else {
						// 若是上传上来的文件
						if (itemName != null && !itemName.equals("")) {
							String uploadDirFullName = request.getServletContext()
									.getRealPath(
											File.separator + "asset"
													+ File.separator + "upload");
							String targetFileFullName = uploadDirFullName
									+ File.separator + DataUtil.getUUID() + "--"
									+ item.getName(); // 增加文件前缀，避免文件重名冲突。中间用“--”隔开。
							if ((new File(uploadDirFullName)).exists() == false)
								(new File(uploadDirFullName)).mkdirs(); // 检查上传临时文件是否存在，不存在则创建。
							// 将上传上来的文件流输出到目标文件！
							java.io.File targetFile = new java.io.File(
									targetFileFullName);
							item.write(targetFile);
							rObj.put(itemName, targetFileFullName);
							LogUtil.debug("[HttpUtil] 上传文件到：" + targetFileFullName
									+ "    文件类型：" + item.getContentType()
									+ "    文件大小：" + item.getSize());
						} else {
							// 若上传上来的文件的名字为空的，当做文件上传失败
							rObj.put(itemName, "");
						}
					}
				}
			}
		} catch (Exception e) {
			LogUtil.exception(e);
		}
		return rObj;
	}

	/**
	 * GetParameters方法中，上传大文件时上传进度的侦听方法。上传进度情况，放置在Session中（原生非分布式Session中）
	 * 数据格式形为：计划上传字节数（可能为-1）| 已上传字节数 | 当前上传第几条。
	 */
	public class GetParametersProgressListener implements ProgressListener {  
		private HttpSession session;  
	    public GetParametersProgressListener(HttpServletRequest request) {   session=request.getSession();    } 
	    public void update(long pBytesRead, long pContentLength, int pItems) {  
	    	LogUtil.debug("upload total: "+pContentLength+" done:"+pBytesRead+" item:"+pItems);
	    	String processStr = pContentLength+"|"+pBytesRead+"|"+pItems;
	    	session.setAttribute("HttpUtil.GetParameters.ProgressData", processStr);
	    }
	}
	
	
	
	
	/**
	 * 设置Cookie的值。 eg: HttpUtil.setCookieValue(response, "UserAccount", "geek",
	 * 2*365*24*60*60);
	 * @param response Request对象
	 * @param name Cookie名称
	 * @param value Cookie取值
	 * @param maxAge Cookie生命周期，以秒为单位（<0表示不设置生命期）
	 */
	public static void setCookieValue(HttpServletResponse response,
			String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		if (maxAge > 0)
			cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}

	
	
	/**
	 * 获得Cookie的值。获取失败，返回NULL。大小写敏感。 eg: String clientID =
	 * HttpUtil.getCookieValue(request, "UserAccount");
	 * 
	 * @param request
	 *            Request对象
	 * @param name
	 *            Cookie名称
	 * @return Cookie取值字符串
	 */
	public static String getCookieValue(HttpServletRequest request, String name) {

		if (name == null)
			name = "";

		Cookie[] cookies = request.getCookies();
		if (null != cookies) {
			for (Cookie cookie : cookies) {
				String cookieName = cookie.getName();
				if (cookieName != null && name.equals(cookieName)) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	
	
	/**
	 * 获得本次访问Http请求的完整URL地址
	 * 
	 * @param request
	 * @param ifIncParams
	 *            是否包含参数部分
	 * @return 完整的访问URL
	 */
	public static String getRequestOriURL(HttpServletRequest request,
			Boolean ifIncParams) {
		String strBackUrl = "http://" + request.getServerName() // 服务器地址
				+ ":" + request.getServerPort() // 端口号
				+ request.getContextPath() // 项目名称
				+ request.getServletPath(); // 请求页面或其他地址
		if (ifIncParams && request.getQueryString() != null)
			strBackUrl = strBackUrl + "?" + (request.getQueryString()); // 参数
		return strBackUrl;
	}

	
	
	/**
	 * 为打印调试获取传输过来的所有POST和GET信息。注意的是，当获取数据后，InputStream会被取出，后续程序的执行会受影响。
	 * 对于FORM表单 application/x-www-form-urlencoded 或 multipart/form-data 均打印出来查看。
	 * eg: LogUtil.log("====ALL POST DATA===="+com.hbbc.util.HttpUtil.
	 * getAllGetAndPostDataForDebug(ctx.request));
	 */
	public static String getAllGetAndPostDataForDebug(HttpServletRequest request)
			throws Exception {
		String rStr = "[GETDATA] " + getRequestOriURL(request, true);
		rStr = rStr
				+ "    [POSTDATA] "
				+ IOUtil.covertInputStream2String(request.getInputStream(),
						"utf-8");
		return rStr;
	}

	
	
	
	/**
	 * 获得本次访问的项目根目录地址，包括：服务器地址+端口号+项目名称。 eg: String baseUrl =
	 * HttpUtil.getRequestBaseURL(request);
	 * 
	 * @param request
	 * @return String
	 */
	public static String getRequestBaseURL(HttpServletRequest request) {
		String url = "http://" + request.getServerName() // 服务器地址
				+ ":" + request.getServerPort() // 端口号
				+ request.getContextPath(); // 项目名称
		return url;
	}
	
	
	

	/**
	 * 判断一个服务器的某个端口是否可用。最后一个参数为超时时间，单位：毫秒。
	 * eg:  System.out.println(HttpUtil.ifServerPortActive("home.handbbc.com", 8369, 1000));
	 * eg:  System.out.println(HttpUtil.ifServerPortActive("192.168.1.11", 80, 1000));
	 */
	public static boolean ifServerPortActive(String address, int port, int timeout){
	    try {  
	    	InetAddress addr = InetAddress.getByName(address);  
	        Socket socket = new Socket();  
	        socket.connect( new InetSocketAddress( addr, port ), timeout);  
	        socket.close();
	        return true;
	    } catch (Exception e) {
	         return false;
	    } 
	}

	
	/**
	 * 检测URL是否可用，从URL中解析出域名/IP 和端口号（不写的取默认值80），测试其端口是否开启。
	 * eg:	 System.out.println(HttpUtil.ifServerPortActive("http://www.badu.com/9090.html", 1000));
	 *	eg: System.out.println(HttpUtil.ifServerPortActive("http://192.168.1.91:9890", 1000));
	 */
	public static boolean ifServerPortActive(String url, int timeout){
		try{
			URL urlobj = new URL(url);
			String host = urlobj.getHost();
			int port = urlobj.getPort();
			if(port==-1) port = 80;
			return ifServerPortActive(host, port, timeout);
		}catch(Exception e){
			return false;
		}
	}


}
