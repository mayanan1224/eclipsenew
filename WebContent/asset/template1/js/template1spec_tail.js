

//上传组件初始化JS代码
jQuery(function($) {
	
	//文件上传组件初始化脚本
	$('.ace-file').ace_file_input({
		no_file:'...',
		btn_choose:'选择文件',
		btn_change:'重新选择',
		droppable:false,
		onchange:null,
		thumbnail:false //| true | large
		//whitelist:'gif|png|jpg|jpeg'
		//blacklist:'exe|php'
		//onchange:''
		//
	});
	
	
	//日期选框 初始化脚本
	$('.ace-datepicker').datepicker({autoclose:true}).next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
	
	
	//时间选框 初始化脚本
	$('.ace-timepicker').timepicker({
		minuteStep: 1,
		showSeconds: true,
		showMeridian: false
	}).next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
	
	
});