
/**
 * 快速AJAX调用封装
 * eg: var postData = { delUserIDs : idString };
* 		 doAjaxAction("acl.UserList.delete.hf",  postData");
 */
function doAjaxAction(actionURL, postData){
	//ajax调用处理函数
	$.ajax({
			url : actionURL, 
			dataType : "Json", 
			data : postData, // { postDataName : postDataValue }, 
			Type : "Post", 
			success : function(data){
				var json = eval(data);
				if (json.result == "Success") {
					if(json.url!=''){
						window.location.href = json.url;
					}else{
						$.gritter.add({title : '消息提醒', text : json.message, class_name : 'gritter-info gritter-center' });
					}
				} else {
					$.gritter.add({ title : '消息提醒', text : json.message, class_name : 'gritter-error gritter-center'});
				}
			}
	});
}


/**
 * 快速AJAX调用封装。发起Ajax调用，数据默认采用Form的数据。
 * // 需要注意的是，对于FORM的两种类型 application/x-www-form-urlencoded（默认） 和 multipart/form-data。后者需要手工在后台处理参数。
 * 
 * eg: doAjaxActionWithFormData("postForm", "post"); 
 */
function doAjaxActionWithFormData(formID, methodType){
	//通过数据验证，准备提交保存
	$("#"+formID).ajaxSubmit({
		type : methodType,
		dataType : "json",
		data : {},
		success : function(data){
			var json = eval(data);
			if (json.result == "Success") {
				if(json.url!=''){
					window.location.href = json.url;
				}else{
					$.gritter.add({title : '消息提醒', text : json.message, class_name : 'gritter-info gritter-center' });
				}
			} else {
				$.gritter.add({ title : '消息提醒', text : json.message, class_name : 'gritter-error gritter-center'});
			}
		}
	});
}



/**
 * 快速AJAX调用封装，提交前会先要求用户进行确认。
 * eg: var postData = { delUserIDs : idString };
* 		 doAjaxActionWithConfirm("acl.UserList.delete.hf",  postData, "确定删除？");
 */
function doAjaxActionWithConfirm(actionURL, postData, confirmMsg) {
	//弹出确认窗口
	
	bootbox.setDefaults({locale : "zh_CN"});
	bootbox.confirm(confirmMsg, function(result) {
		//确定要进行操作的
		if (result) {
			doAjaxAction(actionURL, postData);
		}
	});
}
