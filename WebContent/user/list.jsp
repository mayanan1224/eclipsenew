<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>用户管理</title>
 <jsp:include page="/asset/template1/inc/list-head.htm"></jsp:include>
<style type="text/css">
    #tablehead {
        color: black;
        font-size: 13px
    }
</style>
</head>
<body>
<div class="page-header" style="margin-left:15px;margin-top:8px;">
    <h1 id="title-style">用户管理</h1>
</div>
<button class="btn btn-primary btn-sm t1-menuadj"
        style="margin-left: 15px;" id="add">
    <i class="icon-plus-sign bigger-110"></i> <span
        class="bigger-110 no-text-shadow">新增</span>
</button>

<button class="btn btn-danger btn-sm"
        style="margin-left:15px;margin-top:-5px;" id="del">
    <i class="icon-trash bigger-110"></i> <span
        class="bigger-110 no-text-shadow">删除</span>
</button>
<button class="btn btn-info btn-sm"
        style="margin-left:15px;margin-top:-5px;" id="change">
    <i class="icon-pencil bigger-110"></i> <span
        class="bigger-110 no-text-shadow">修改</span>
</button>
<!-- 页面区 -->
<div class="page-content">
    <div class="row">
    <form id="myForm" action="${formAction }" method="post">
        <div class="table-responsive" style="margin-left:8px">
            <!-- 列表表头 -->
            <table id="data-table-1"
                   class="table table-striped table-bordered table-hover">
                <thead>
                <tr>
                    <th class="center"><label><input type="checkbox"
                                                     class="ace"/><span class="lbl"></span></label></th>
                    <th class="center" id="tablehead">编号</th>
                    <th class="center" id="tablehead">姓名</th>
                    <th class="center" id="tablehead">性别</th>
                    <th class="center" id="tablehead">年龄</th>                 
                    <th class="center" id="tablehead">标志</th>                   
                    <th class="center" id="tablehead">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${users}" var="user">
                    <tr>
                        <td class="center"><label> <input type="checkbox" class="ace"/> <span class="lbl"></span></label></td>
                        <td class="center">${user.id }</td> 
                        <td class="center">${user.name}</td> 
                        <td class="center">${user.sex}</td>                        
                        <td class="center">${user.age}</td>
                        <td class="center"><fmt:formatDate value="${user.seff}" pattern="yyyy-MM-dd HH:mm:ss"/></td>                     
                        <td class="center">
                        <a class="modify green showModify function-modify" style="cursor:pointer">
                            <i class="icon-pencil bigger-130 "></i>
                        </a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <%@ include file="/asset/template1/page/page1.jsp" %>
        </div>
        </form>
    </div>
</div>
</body>
<script src="asset/template1/js/template1.1.1tail.js"></script>
<script type="text/javascript">
	$("#add").click(function(){
		window.location.href="com.rj.bd.user.controler.userControler.add.hf";
	});
	$(".function-modify").click(function() {
		var goodsclassifyID = $(this).parent().parent().children().eq(1).text();
		window.location.href = "com.rj.bd.user.controler.userControler.update.hf?id="+id;
	});
	$("#change").click(function(){
		var ase = $(".ace:checked");
		if(ase.length>1){
			$.gritter.add({
		        title : '错误提示',
		        text : '只能选择一项进行操作哦！',
		        class_name : 'gritter-error gritter-center'
		    });
		    return;
		}else{
			var str = "";
			for(var i = 0; i < ase.length; i++){
				var value = ase.eq(i).parent().parent().parent().children("td").eq(1).text();
				if(value!=null && value!=""){
					if(i==ase.length-1){
						str += value;
					}else{
						str += value + ",";
					}
				}	
			}		
			if(str==""){
				$.gritter.add({
			        title : '错误提示',
			        text : '请至少选择一项操作',
			        class_name : 'gritter-error gritter-center'
			    });
			    return;
			}
			console.log(str);
			window.location.href = "com.rj.bd.user.controler.userControler.update.hf?id="+str;
		}		
		
	});
	$("#del").click(function(){
		var ase = $(".ace:checked");
		var str = "";
		for(var i = 0; i < ase.length; i++){
			var value = ase.eq(i).parent().parent().parent().children("td").eq(1).text();
			if(value!=null && value!=""){
				if(i==ase.length-1){
					str += value;
				}else{
					str += value + ",";
				}
			}	
		}		
		if(str==""){
			$.gritter.add({
		        title : '错误提示',
		        text : '请至少选择一项操作',
		        class_name : 'gritter-error gritter-center'
		    });
		    return;
		}
		console.log(str);
		var data = {"id":str};
		doAjaxActionWithConfirm("com.rj.bd.user.controler.userControler.delete.hf", data, "确定执行删除操作？");
	})
	</script>
</html>