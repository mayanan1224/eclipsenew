package com.rj.bd.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;


/**
 * 分布式计算/云计算能力 工具类
 */
public class DistUtil {
	
	private static String gDS_StorageType = "";				//DB, AliOSS, DB+AliOSS
	private static String gDS_ServerBaseURL = "";			//http://127.0.0.1:8080/sjkbo/asset/distfile/  或    http://sjkcdn.cssbbc.com
	
	private static String gDS_File_StorageBasePath = ""; ///Users/hbbc/temp/demobo
	
	private static String gDS_DB_DBUtilName = "";			//Default
	
	private static String gDS_OSS_EP = "";						//http://oss-cn-qingdao.aliyuncs.com
	private static String gDS_OSS_ID = "";						//3oYdBCbrTOpTH...
	private static String gDS_OSS_Key = "";					//zRAqjOiYTl9A8NuzaREQGDR6Y6W...
	private static String gDS_OSS_BucketName = "";		//hbbctest
	
	private static String gDS_Session_DBName="";			//Default
	private static String gDS_Session_DomainName ="";	//www.xxx.com
	private static String gDS_Session_Timeout="";			//1800
	
	public static final String DistFileBasePath = "asset/distfile";
	
	static{
		try{
			
			gDS_StorageType = ConfigUtil.getProperty(null, "DistUtil.DS.StorageType", false);
			gDS_ServerBaseURL = ConfigUtil.getProperty(null, "DistUtil.DS.ServerBaseURL", false);
			
			gDS_File_StorageBasePath =  ConfigUtil.getProperty(null, "DistUtil.DS.File.StorageBasePath", false); 
			if(gDS_File_StorageBasePath.trim().toUpperCase().equals("{DefaultStoragePath}".toUpperCase())){
				String curClassPath = (new IOUtil()).getClass().getResource("/").getFile().toString();
				String curWebRootPath = (new File(curClassPath)).getParentFile().getParentFile().toString();
				gDS_File_StorageBasePath = curWebRootPath+File.separator+"Data";
			}
			
			gDS_DB_DBUtilName = ConfigUtil.getProperty(null, "DistUtil.DS.DB.DBUtilName", false);
			
			gDS_OSS_EP = ConfigUtil.getProperty(null, "DistUtil.DS.OSS.EP", false);
			gDS_OSS_ID = ConfigUtil.getProperty(null, "DistUtil.DS.OSS.ID", false);			
			gDS_OSS_Key = ConfigUtil.getProperty(null, "DistUtil.DS.OSS.Key", false);
			gDS_OSS_BucketName = ConfigUtil.getProperty(null, "DistUtil.DS.OSS.BucketName", false);
			
			gDS_Session_DBName=ConfigUtil.getProperty(null, "DistUtil.Session.DBUtilName", false);
			gDS_Session_DomainName=ConfigUtil.getProperty(null, "DistUtil.Session.DomainName", false);
			gDS_Session_Timeout=ConfigUtil.getProperty(null, "DistUtil.Session.Timeout", false);
			
		}catch(Exception e){
			LogUtil.exception(e);
		}
	}
	
	
	
	/**
	 * 获得本地文件存储的根路径
	 * eg: String fileName = DistUtil.getStorageBasePath()+File.separator+"a.png";
	 */
	public static String getStorageBasePath(){
		return gDS_File_StorageBasePath;
	}
	
	
	
	/**
	 * 将文件存放到分布式存储中。根据配置文件中的"DistUtil.DS.ServerBaseURL" 判定是否保存到数据库中 和 是否保存到阿里云存储中。
	 * 执行成功返回fileID，如“88d7e49a-4e3e-48ec-a398-40bdae4a45f4.jpg”任意错误返回空字符串 ""。若同时保存到DB 和 AliOSS任一发生错误，均视做保存失败。
	 * eg: String fileID = DistUtil.putFileToDS("/Users/hbbc/Temp/aa.jpg");
	 * 
	 * @param sourceFileFullName  文件全路径名
	 * @return FileID，失败返回空
	 * @throws Exception
	 */
	public static String putFileToDS(String sourceFileFullName) {
		boolean dbFlag = false;
		boolean aliossFlag = false;
		boolean fileFlag = false;
		if(gDS_StorageType.toUpperCase().indexOf("FILE")>=0) fileFlag = true;
		if(gDS_StorageType.toUpperCase().indexOf("DB")>=0) dbFlag = true;
		if(gDS_StorageType.toUpperCase().indexOf("ALIOSS")>=0) aliossFlag = true;
		return putFileToDS(sourceFileFullName, fileFlag, dbFlag, aliossFlag);
	}
	
	
	
	
	/**
	 * 将文件存放到分布式存储中。手工指定 fileFlag（是否保存在本地服务器硬盘中），dbFlag（是否保存到数据库中）， aliossFlag（是否保存到阿里云存储中）。
	 * 执行成功返回fileID，如“88d7e49a-4e3e-48ec-a398-40bdae4a45f4.jpg”任意错误返回空字符串 ""。若同时保存时，当File、DB 和 AliOSS任一发生错误，均视做保存失败。
	 * eg: String fileID = DistUtil.putFileToDS("/Users/hbbc/Temp/aa.jpg", true, true, false);
	 * 需要注意的是，上传文件大小尽量不要超过5M。目前文件方式不限、DB方式测试200M、阿里云方式为5M左右（尚可优化）。
	 * 
	 * @param sourceFileFullName  文件全路径名
	 * @param fileFlag  是否保存服务器的硬盘中
	 * @param dbFlag  是否保存到数据中
	 * @param aliossFlag  是否保存到阿里云中
	 * @return FileID，失败返回空
	 * @throws Exception
	 */
	public static String putFileToDS(String sourceFileFullName, boolean fileFlag, boolean dbFlag, boolean aliossFlag) {
		try{
			String fileExtension = IOUtil.getFileExtension(sourceFileFullName);
			String fileID = DataUtil.getUUID()+"."+fileExtension;
			File file = new File(sourceFileFullName);
			
			// 写到服务器硬盘的指定目录中
			if(fileFlag){
				String targetFileFullName = gDS_File_StorageBasePath+File.separator+fileID;
				if((new File(gDS_File_StorageBasePath)).exists()==false) (new File(gDS_File_StorageBasePath)).mkdirs();		//检查目标目录是否存在，不存在的话，自动创建
				boolean bFlag = IOUtil.copyFile(sourceFileFullName, targetFileFullName);
				if(bFlag==false) return "";
			}
			
			// 写文件到数据库
			if(dbFlag){
				//准备数据
				FileInputStream fis = new FileInputStream(file);
					   
				//打开数据库连接
				DBUtil dbobj = DBUtil.getInstance(gDS_DB_DBUtilName);
				Connection conn = dbobj.openConnection();
					
				//准备执行SQL语句
				PreparedStatement pstat = conn.prepareStatement("INSERT INTO sys_diststoragedata(FileID, FileData) VALUES(?, ?)");  
				pstat.setString(1,  fileID);
				pstat.setBinaryStream(2, fis, file.length()); 
			    int rvalue = pstat.executeUpdate();
			    LogUtil.log("[DBUtil_DistUtil] 执行Execute语句："+pstat.toString()+"        RESULT="+rvalue);
			    
			    //关闭数据库连接
				dbobj.closeConnection();
				if(rvalue<=0){
					LogUtil.log("[DistUtil] 保存FileID="+fileID+" 到分布式文件存储（DB）发生错误，保存失败！");
					return "";
				}
			}
			
			//上传文件到阿里云OSS
			if(aliossFlag){
				try{
				// 创建OSSClient对象。
				OSSClient client = new OSSClient(gDS_OSS_EP, gDS_OSS_ID, gDS_OSS_Key);
				//类别参数
				ObjectMetadata objectMeta = new ObjectMetadata();
				objectMeta.setContentLength(file.length());
				objectMeta.setContentType(IOUtil.getMimeType(sourceFileFullName));
				//上传文件
				InputStream input = new FileInputStream(sourceFileFullName);
				PutObjectResult rObj = client.putObject(gDS_OSS_BucketName, fileID, input, objectMeta);
				LogUtil.log("[DistUtil] 上传文件到阿里云OSS："+fileID+"        RESULT="+rObj.getETag());
				}catch(Exception ee){
					LogUtil.log("[DistUtil] 保存FileID="+fileID+" 到分布式文件存储（AliOSS）发生错误，保存失败！  Exception: "+ee.toString());
					return "";
				}
			}
			
			// 注册分布式文件
			String regsql = "INSERT INTO sys_diststorage(FileID, FileExtend, FileFlag, DBFlag, AliOSSFlag, OriFileFullName, OriFileSize) VALUES('"+
						fileID+"', '"+ fileExtension+"', "+(fileFlag?1:0)+", "+(dbFlag?1:0)+", "+(aliossFlag?1:0)+", '"+sourceFileFullName+"', '"+file.length()+"');";
			DBUtil.getInstance(gDS_DB_DBUtilName).execute(regsql);
			return fileID;
			
		}catch(Exception e){
			LogUtil.exception(e);
			return "";
		}
	}
	
	public static String putFile(String sourceFileFullName,int systemID) {
		boolean dbFlag = false;
		boolean aliossFlag = false;
		boolean fileFlag = false;
		if(gDS_StorageType.toUpperCase().indexOf("FILE")>=0) fileFlag = true;
		if(gDS_StorageType.toUpperCase().indexOf("DB")>=0) dbFlag = true;
		if(gDS_StorageType.toUpperCase().indexOf("ALIOSS")>=0) aliossFlag = true;
		return putFileUpload(sourceFileFullName, fileFlag, dbFlag, aliossFlag,systemID);
	}
	public static String putFileUpload(String sourceFileFullName, boolean fileFlag, boolean dbFlag, boolean aliossFlag,int systemID) {
		try{
			String fileExtension = IOUtil.getFileExtension(sourceFileFullName);
			String fileID = DataUtil.getUUID()+"."+fileExtension;
			fileID=systemID+"_"+fileID;
			File file = new File(sourceFileFullName);
			String UPLOAD_DIR = "/server/data/spsycret";
			// 写到服务器硬盘的指定目录中
			if(fileFlag){
				
				String targetFileFullName = UPLOAD_DIR+File.separator+fileID;
				if((new File(UPLOAD_DIR)).exists()==false) (new File(UPLOAD_DIR)).mkdirs();		//检查目标目录是否存在，不存在的话，自动创建
				boolean bFlag = IOUtil.copyFile(sourceFileFullName, targetFileFullName);
				if(bFlag==false) return "";
			}
			
			// 写文件到数据库
			if(dbFlag){
				//准备数据
				FileInputStream fis = new FileInputStream(file);
					   
				//打开数据库连接
				DBUtil dbobj = DBUtil.getInstance(gDS_DB_DBUtilName);
				Connection conn = dbobj.openConnection();
					
				//准备执行SQL语句
				PreparedStatement pstat = conn.prepareStatement("INSERT INTO sys_diststoragedata(FileID, FileData) VALUES(?, ?)");  
				pstat.setString(1,  fileID);
				pstat.setBinaryStream(2, fis, file.length()); 
			    int rvalue = pstat.executeUpdate();
			    LogUtil.log("[DBUtil_DistUtil] 执行Execute语句："+pstat.toString()+"        RESULT="+rvalue);
			    
			    //关闭数据库连接
				dbobj.closeConnection();
				if(rvalue<=0){
					LogUtil.log("[DistUtil] 保存FileID="+fileID+" 到分布式文件存储（DB）发生错误，保存失败！");
					return "";
				}
			}
			
			//上传文件到阿里云OSS
			if(aliossFlag){
				try{
				// 创建OSSClient对象。
				OSSClient client = new OSSClient(gDS_OSS_EP, gDS_OSS_ID, gDS_OSS_Key);
				//类别参数
				ObjectMetadata objectMeta = new ObjectMetadata();
				objectMeta.setContentLength(file.length());
				objectMeta.setContentType(IOUtil.getMimeType(sourceFileFullName));
				//上传文件
				InputStream input = new FileInputStream(sourceFileFullName);
				PutObjectResult rObj = client.putObject(gDS_OSS_BucketName, fileID, input, objectMeta);
				LogUtil.log("[DistUtil] 上传文件到阿里云OSS："+fileID+"        RESULT="+rObj.getETag());
				}catch(Exception ee){
					LogUtil.log("[DistUtil] 保存FileID="+fileID+" 到分布式文件存储（AliOSS）发生错误，保存失败！  Exception: "+ee.toString());
					return "";
				}
			}
			
			// 注册分布式文件
			String regsql = "INSERT INTO sys_diststorage(FileID, FileExtend, FileFlag, DBFlag, AliOSSFlag, OriFileFullName, OriFileSize) VALUES('"+
						fileID+"', '"+ fileExtension+"', "+(fileFlag?1:0)+", "+(dbFlag?1:0)+", "+(aliossFlag?1:0)+", '"+sourceFileFullName+"', '"+file.length()+"');";
			DBUtil.getInstance(gDS_DB_DBUtilName).execute(regsql);
			return fileID;
			
		}catch(Exception e){
			LogUtil.exception(e);
			return "";
		}
	}
	
	/**
	 * 从分布式存储中读取文件。查看数据库中该分布式文件的设置，若是DB方式存储的，从DB获得。否则，从阿里云上获取。
	 * 获取下来的文件，会自动检查起尺寸和上传时的原始尺寸是否相同，若相同认为获取成功，返回True，否则，返回False。
	 * eg: boolean a = DistUtil.getFileFromDS("05cf10b1-1532-43b3-b558-97911fb06fd5.jpg", "/Users/hbbc/Temp/aa_1.jpg");
	 * 
	 * @param fileID 文件标识
	 * @param targetFileFullName 目标文件存放路径和名称
	 * @return 成功返回True，失败返回False
	 */
	public static boolean getFileFromDS(String fileID, String targetFileFullName) {
		
		boolean bFlag = false;
		try{
			//读取文件存放状态
			DataTable dt = DBUtil.getInstance(gDS_DB_DBUtilName).query("SELECT FileID, FileExtend, FileFlag, DBFlag, AliOSSFlag, OriFileFullName, OriFileSize, AddTime FROM sys_diststorage WHERE FileID='"+fileID+"'");
			if(dt.isNull()) return false;
			long oriFileSize = Integer.parseInt(dt.getCell(0, "OriFileSize").toString());
			boolean fileFlag = Boolean.parseBoolean(dt.getCell(0, "FileFlag").toString());
			boolean dbFlag = Boolean.parseBoolean(dt.getCell(0, "DBFlag").toString());
			boolean aliossFlag = Boolean.parseBoolean(dt.getCell(0, "AliOSSFlag").toString());
			
			//若在服务器硬盘上有存放，尝试从其中获取
			if(fileFlag){
				String storageFileFullName = gDS_File_StorageBasePath+File.separator+fileID;
				if(new java.io.File(storageFileFullName).exists()){
					boolean cFlag = IOUtil.copyFile(storageFileFullName, targetFileFullName);
					if(cFlag){
			            //文件写完后，比较文件大小，和之前记录的一致才认为成功
						File tFile =new File(targetFileFullName);
						if((tFile).exists() && tFile.length()==oriFileSize) bFlag = true;
					}
				}else{
					LogUtil.log("文件："+storageFileFullName+" 不存在！");
				}
			}
			
			//若从服务器硬盘上获取失败，且在服务器上有备份，则，尝试从DB中读取。
			if(bFlag==false && dbFlag){
				//打开数据库连接
				DBUtil dbobj = DBUtil.getInstance(gDS_DB_DBUtilName);
				Connection conn = dbobj.openConnection();
				//从数据库中获取文件，并写入文件
				PreparedStatement stat = conn.prepareStatement("SELECT FileID, FileData FROM sys_diststoragedata WHERE FileID=?");  
				stat.setString(1, fileID);
		        ResultSet result = stat.executeQuery();  
		        LogUtil.log("[DBUtil_DistUtil] 执行Execute语句："+stat.toString());
		        if (result.next()) {
		            InputStream in = result.getBinaryStream("FileData");  
		            FileOutputStream out = new FileOutputStream(targetFileFullName);  
		            byte[] buffer = new byte[1024];		// 每次读取1k  
		            for (int len = 0; (len = in.read(buffer)) > 0;) {  
		                out.write(buffer, 0, len);  
		            }
		            in.close();
		            out.close();
		            //文件写完后，比较文件大小，和之前记录的一致才认为成功
					File tFile =new File(targetFileFullName);
					if((tFile).exists() && tFile.length()==oriFileSize) bFlag = true;
		        }else{
		        	bFlag = false;
		        }
		    	//关闭数据库连接
				dbobj.closeConnection();
			}
			
			//若从DB未读取或者读取失败，且alioss上存放有，从alioss上读取
			if(bFlag==false && aliossFlag){
				File file =new File(targetFileFullName);
			    OSSClient client = new OSSClient(gDS_OSS_EP, gDS_OSS_ID, gDS_OSS_Key);
			    client.getObject(new GetObjectRequest(gDS_OSS_BucketName, fileID), file);
	            //文件写完后，比较文件大小，和之前记录的一致才认为成功
				File tFile =new File(targetFileFullName);
				if((tFile).exists() && tFile.length()==oriFileSize) bFlag = true;
			}
			
			//刷新改分布式文件的最近访问时间和访问次数，以便于统计
			if(bFlag)DBUtil.getInstance(gDS_DB_DBUtilName).execute("UPDATE sys_diststorage SET LastVisitTime='"+DataUtil.getNowTimeString()+"', VisitCount = VisitCount+1  WHERE FileID='"+DataUtil.getDBSafeString(fileID)+"'");
			
		}catch(Exception e){
			LogUtil.exception(e);
			bFlag = false;
		}
		return bFlag;
	}
	
	

	
	
	/**
	 * 显示之前使用此方法获取该FileID的正确的URL地址，如：http://127.0.0.1:8080/sjkbo/asset/distfile/05cf10b1-1532-43b3-b558-97911fb06fd5.jpg
	 * eg: System.out.println(DistUtil.getDownloadURL(request, "05cf10b1-1532-43b3-b558-97911fb06fd5.jpg"));
	 * eg: System.out.println(DistUtil.getDownloadURL(null, "05cf10b1-1532-43b3-b558-97911fb06fd5.jpg"));
	 * 
	 * @param request 本次页面访问的Request对象。可以为null，此时计算“{DefaultDistBaseDir}”路径时会出现问题。
	 * @param fileID 分布式文件ID
	 * @return  通过WEB获取该文件的正确URL地址
	 */
	public static String getDownloadURL(HttpServletRequest request, String fileID) {
		
		if(CurServerBaseURL==null){
			CurServerBaseURL = gDS_ServerBaseURL;
			if(request!=null && gDS_ServerBaseURL.trim().toUpperCase().equals("{DefaultDistBaseDir}".toUpperCase())){
				//若是默认路径，则计算出默认下载地址，并赋值
				CurServerBaseURL = "http://" + request.getServerName();	 // 服务器地址
				if(request.getServerPort()!=80)	CurServerBaseURL+=  ":" + request.getServerPort(); 	// 端口号，若是80端，则略去
				CurServerBaseURL += request.getContextPath();				 // 项目名称
				CurServerBaseURL += "/"+DistFileBasePath;
			}
		}
		//返回正确下载地址
		return CurServerBaseURL+"/"+fileID;
	}
	public static String CurServerBaseURL=null;
	
	
	
	
	/**
	 * WEB专用方法。  获取分布式文件，存储在本地分布式文件缓存文件夹。并直接Response输出本次获取的文件内容。
	 * 本方法一般在 404 错误友好提示页面调用，作为分布式文件本地缓存获取的入口。文件获取成功返回True；文件不存在或者获取错误返回False。
	 * 
	 * @param url 获取文件的完整路径，如：http://127.0.0.1:8080/sjkbo/asset/distfile/19f45d6a-acda-4f2b-aa3b-758623100486.jpg
	 * @param request 
	 * @param response
	 * @return 获取成功返回True，获取失败返回False。
	 */
	public static boolean checkAndDownloadDSFile(String url, HttpServletRequest request , HttpServletResponse response){
		try {
			//检查传输过来的参数，若不是 asset/distfile 的，则直接返回失败
			if(url.indexOf(DistFileBasePath)<0) return false;
			
			//解析参数
			String fileID = url.substring(url.lastIndexOf("/")+1);
			String serverSaveDir = request.getSession().getServletContext().getRealPath("/")+"asset"+File.separator+"distfile";	//存储位置
			String targetFileFullName = serverSaveDir+File.separator+fileID;
			File file = new File(targetFileFullName);
			LogUtil.debug("[DistFileFilter] 获取文件请求："+url+"    FileID="+fileID+"    TargetFileFullName="+targetFileFullName);
			if(new File(serverSaveDir).exists()==false)(new File(serverSaveDir)).mkdir();		//检查存储文件夹是否存在，若不存在，则创建
			
			//从分布式存储中获取文件到指定路径
			boolean bFlag = DistUtil.getFileFromDS(fileID, targetFileFullName);
			if(bFlag==false) {
				LogUtil.debug("[DistFileFilter] 获取分布式文件失败，FileID="+fileID);
				return false;
			}
			
			//获取成功，直接返回（否则，第一次无法显示出来）
			InputStream fis = new BufferedInputStream(new FileInputStream(file));
			OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
			byte[] buffer = new byte[fis.available()];
			fis.read(buffer);
			fis.close();
			response.reset();
			response.setContentType(IOUtil.getMimeType(targetFileFullName));
			response.setHeader("Content-Disposition", "inline; filename=" + file.getName());		//在线打开方式。若是下载，则是：response.setHeader("Content-Disposition", "attachment; filename=" + f.getName());
			toClient.write(buffer);
			toClient.flush();
			toClient.close();
			//执行成功
			return true;
		} catch (Exception e) {
			LogUtil.exception(e);
		}
		return false;
	}
	
	
	
	
	
	/**
	 * 检查分布式数据表是否创建，若没有创建，自动创建数据表。一般在写入分布式数据表发生异常时调用。先检查表是否存在，若不存在，则创建数据表。
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	private static void checkAndBuildDSDBTable() throws Exception{
		
		//若查询出现异常，判断是否是分布式存储数据表没有建立，若没有建立，自动建立起来！
		String tableStr = DBUtil.getInstance(gDS_DB_DBUtilName).queryReturnSingleString("select table_name from `INFORMATION_SCHEMA`.`TABLES` where table_name ='sys_diststorage' and TABLE_SCHEMA=DATABASE();");
		if(tableStr==null){ 
			//确实没有建数据表的，建立数据表
			String sql1 = "CREATE TABLE `sys_diststorage` (  `FileID` varchar(200) NOT NULL COMMENT '文件标识，UUID',  `FileExtend` varchar(50) DEFAULT NULL COMMENT '文件扩展名',  `FileFlag` bit(1) DEFAULT NULL COMMENT '文件是否存放在本地磁盘中，若有存放，将优先于 DB 和 AliOSS 方式被获取到',  `DBFlag` bit(1) DEFAULT NULL COMMENT '文件是否存放在 数据库的标记（获取的时候，优先从DB上获取，而非阿里云上）',  `AliOSSFlag` bit(1) DEFAULT NULL COMMENT '文件是否存放在阿里云上的标记',  `OriFileFullName` varchar(255) DEFAULT NULL COMMENT '文件的原始路径名称',  `OriFileSize` int(11) DEFAULT NULL COMMENT '原始文件尺寸（字节），文件获取下来后，会与此此段进行比较，作为判断文件获取是否成功的依据。',  `VisitCount` int(11) DEFAULT '0' COMMENT '文件被访问次数',  `LastVisitTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '文件最近一次被访问时间',  `AddTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',  PRIMARY KEY (`FileID`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;";		
			String sql2 = "CREATE TABLE `sys_diststoragedata` (  `FileID` varchar(200) NOT NULL,  `FileData` longblob,  `AddTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',  PRIMARY KEY (`FileID`)	) ENGINE=MyISAM DEFAULT CHARSET=utf8; ";
			String sql3 = "SET GLOBAL max_allowed_packet = 500*1024*1024;";
			DBUtil.getInstance(gDS_DB_DBUtilName).execute(sql1);
			DBUtil.getInstance(gDS_DB_DBUtilName).execute(sql2);
			DBUtil.getInstance(gDS_DB_DBUtilName).execute(sql3);
		}
	}
	
	
	
	
	
	
	//==Dist Session===================================================================
	
	

	
	
	/**
	 * 获得分布式Session存储中存储的Session值。目前分布式Session仅支持DB存储方式。
	 * Session的存储数据库名、客户端存储的Cookie名称、Session的过期时间，可以在配置文件中进行配置，
	 * 此功能的正常运行，需要先在数据库中建立名为：sys_distsession的数据表。
	 * 获取失败，返回空字符串“”。
	 * eg: System.out.println("getDistSession="+DistUtil.getDistSession("dd", ctx.request));
	 * 
	 * @param sessionName session 的名字，长度不超过64位。
	 * @param request 页面的输入对象
	 * @return 返回Session值，没有数值或者获取失败，均返回空字符串 “”
	 * @throws Exception
	 */
	public static String getDistSession(String sessionName,  HttpServletRequest request) throws Exception{
		String clientID = HttpUtil.getCookieValue(request, getDistSessionCookieKey(request));
		if(clientID!=null){
			//获取数据库中的Session记录值，考虑Session过期因素！
			DBUtil dbcon = DBUtil.getInstance(gDS_Session_DBName);
			dbcon.SQLLogLevel = 2;
			dbcon.openConnection();
			
			String sessionTimeoutLimitStr = DataUtil.calDatetimeString(new Date(),  -1 * Integer.parseInt(gDS_Session_Timeout));
			String sql1 = "SELECT SessionValue FROM sys_distsession WHERE ClientID='"+clientID+"' AND SessionName='"+sessionName+"'  AND LastVisitTime>'"+sessionTimeoutLimitStr+"'";
			String distSessionValue = dbcon.queryReturnSingleString(sql1);
			//更新该Session参数的最新访问时间
			if(distSessionValue!=null) {
				String sql2 = "UPDATE sys_distsession SET LastVisitTime='"+DataUtil.getNowTimeString()+"' WHERE ClientID='"+clientID+"'";    //需要对这个用户所有的Session数据的有效期进行刷新，而非某个特定的某个
				dbcon.execute(sql2);
				dbcon.closeConnection();
				return distSessionValue;
			}
			//关闭数据库连接
			dbcon.closeConnection();
		}
		return "";
	}
	
	
	
	
	
	/**
	 * 设置分布式存储中的Session对象的数值。目前分布式Session仅支持DB存储方式。
	 * Session的存储数据库名、客户端存储的Cookie名称、Session的过期时间，可以在配置文件中进行配置，
	 * 此功能的正常运行，需要先在数据库中建立名为：sys_distsession的数据表。
	 * eg: DistUtil.setDistSession("dd", "sdf'd", ctx.request, ctx.response);
	 * 
	 * @param sessionName  session 的名字，长度不超过64位。
	 * @param sessionValue 存储的值，长度不限。对“'”等可能会影响SQL语句正常运行的，做了转义。存储二进制数据的时候，建议做BASE64转码后再存储。
	 * @param request 页面请求对象
	 * @param response 页面返回对象
	 * @throws Exception
	 */
	public static void setDistSession(String sessionName, String sessionValue, HttpServletRequest request, HttpServletResponse response) throws Exception{
		//从Cookie中获取以域名作为Cookie名字的Cookie对象。若是没有，则新写入。作为浏览器的唯一标识。
		String clientID = HttpUtil.getCookieValue(request, getDistSessionCookieKey(request));
		if(clientID==null){
			clientID=DataUtil.getUUID();
			HttpUtil.setCookieValue(response,  getDistSessionCookieKey(request), clientID,  2*365*24*60*60);
		}
		//检查数据库中该浏览器的某Session数据是否存在
		DBUtil dbcon = DBUtil.getInstance(gDS_Session_DBName);
		dbcon.SQLLogLevel = 2;
		dbcon.openConnection();
		
		String oldDBSessionValue = null;
		String qsql = "SELECT SessionValue FROM sys_distsession WHERE ClientID='"+clientID+"' AND SessionName='"+sessionName+"'";
		try{
			oldDBSessionValue = dbcon.queryReturnSingleString(qsql);
		}catch(Exception e){
			//若查询出现异常，判断是否是分布式存储数据表没有建立，若没有建立，自动建立起来！
			String tableStr = dbcon.queryReturnSingleString("select table_name from `INFORMATION_SCHEMA`.`TABLES` where table_name ='sys_distsession' and TABLE_SCHEMA=DATABASE();");
			if(tableStr==null){ 
				//确实没有建数据表的，建立数据表
				dbcon.execute("CREATE TABLE `sys_distsession` (`ClientID` varchar(100) NOT NULL COMMENT '客户端标识，是一个UUID。跟浏览器中记录的Cookie相匹配。', `SessionName` varchar(255) NOT NULL COMMENT 'Session名字', `SessionValue` text COMMENT 'Session值', `LastVisitTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次访问时间',  `AddTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',  PRIMARY KEY (`ClientID`,`SessionName`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
				oldDBSessionValue = dbcon.queryReturnSingleString(qsql);
			}else{
				//否则，刚才的异常不予处理，继续抛出
				throw e;
			}
		}
		
		String nowTimeStr = DataUtil.getNowTimeString();
		if(sessionValue==null) sessionValue="";
		if(oldDBSessionValue==null){
			//插入新记录
			String sql = "INSERT INTO sys_distsession(ClientID, SessionName, SessionValue, LastVisitTime, AddTime) Values('"+clientID+"', '"+sessionName+"', '"+DataUtil.getDBSafeString(sessionValue)+"', '"+nowTimeStr+"', '"+nowTimeStr+"');";
			dbcon.execute(sql);
		}else{
			//更新记录
			String sql = "UPDATE sys_distsession SET SessionValue='"+DataUtil.getDBSafeString(sessionValue)+"', LastVisitTime='"+nowTimeStr+"' WHERE ClientID='"+clientID+"' AND SessionName='"+sessionName+"'";
			dbcon.execute(sql);
		}
		
		//关闭数据库连接
		dbcon.closeConnection();
	}
	
	
	
	/**
	 * 计算Session存放时的Cookie的值
	 */
	private static String CurDomainName = null;
	private static String getDistSessionCookieKey(HttpServletRequest request){
		if(CurDomainName==null){
			if(request!=null && gDS_Session_DomainName.trim().toUpperCase().equals("{DefaultSessionCookieName}".toUpperCase())){
				//若是默认路径，则计算出默认下载地址，并赋值
				CurDomainName = request.getServerName().replace(".", "_");	 							// 服务器地址变形
				if(request.getServerPort()!=80)	CurDomainName+=  "_" + request.getServerPort(); 	// 端口号，若是80端，则略去
				CurDomainName += ""+request.getContextPath().replace("/", "_");				 			// 项目名称
			}else{
				CurDomainName = gDS_Session_DomainName;
			}
		}
		return CurDomainName;
	}
	
	
	
/*//建立数据库（勿删） ==================================================================


1、建议分布式文件存储文件注册表
CREATE TABLE `sys_diststorage` (
  `FileID` varchar(200) NOT NULL COMMENT '文件标识，UUID',
  `FileExtend` varchar(50) DEFAULT NULL COMMENT '文件扩展名',
  `FileFlag` bit(1) DEFAULT NULL COMMENT '文件是否存放在本地磁盘中，若有存放，将优先于 DB 和 AliOSS 方式被获取到',
  `DBFlag` bit(1) DEFAULT NULL COMMENT '文件是否存放在 数据库的标记（获取的时候，优先从DB上获取，而非阿里云上）',
  `AliOSSFlag` bit(1) DEFAULT NULL COMMENT '文件是否存放在阿里云上的标记',
  `OriFileFullName` varchar(255) DEFAULT NULL COMMENT '文件的原始路径名称',
  `OriFileSize` int(11) DEFAULT NULL COMMENT '原始文件尺寸（字节），文件获取下来后，会与此此段进行比较，作为判断文件获取是否成功的依据。',
  `VisitCount` int(11) DEFAULT '0' COMMENT '文件被访问次数',
  `LastVisitTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '文件最近一次被访问时间',
  `AddTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`FileID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


2、建立分布式文件存储的文件储存表
CREATE TABLE `sys_diststoragedata` (
  `FileID` varchar(200) NOT NULL,
  `FileData` longblob,
  `AddTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`FileID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

1）解决MySQL的SQL语句长度限制的问题(500M)：
SET GLOBAL max_allowed_packet = 500*1024*1024;
2）表类别为：MyISAM，以解决单行数据不能超过4M 的问题


3、建立分布式Session的数据表
CREATE TABLE `sys_distsession` (
  `ClientID` varchar(100) NOT NULL COMMENT '客户端标识，是一个UUID。跟浏览器中记录的Cookie相匹配。',
  `SessionName` varchar(255) NOT NULL COMMENT 'Session名字',
  `SessionValue` text COMMENT 'Session值',
  `LastVisitTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次访问时间',
  `AddTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`ClientID`,`SessionName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

*/

}


