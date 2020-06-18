
function ACETemplate1Adjust() {
	
	$(".dataTables_wrapper .row:first-child").attr("style","padding-top: 0px;padding-bottom: 12px;background-color: #F9FA33 ;  height: 0px;display:none");
	$(".page-content").attr("style","  background: #fff; margin: 0;padding: 10px 10px -4px;");

	$("#data-table-1>thead tr>th").attr("style", "margin-bottom: 0!important;  color: black;");
	// 3.2 table搜索样式
	//$("#data-table-1_filter label input").attr("placeholder", "搜索(多条件搜索中间用空格隔开)").attr("style", "width:50%");
	$("#data-table-1_filter label input").attr("style", "display:none;width:50%; margin: 0 4px;");
	
	$("#data-table-1_filter label").attr("style", "width:100%");
	$("#data-table-1_filter").attr("style", "margin-right: -16px;");
	
	$("#data-table-1_wrapper .row").first().children().eq(1).attr("style", "margin-top:0px");
	//$("#data-table-1_wrapper").attr("style", "margin-top:-2px");

	// 3.3 增加表单行间距样式
	$("div .modal-body .row:not(#iconr)").attr("style", "margin-top:-14px;");
	$("div .modal-body .row .col-sm-3").attr("style", "margin-top:6px;text-align:right");
	$("div#myModal .modal-body .row i:lt(3)").attr("style", "margin-top:9px;text-align:right;color:gray");

	// 3.6 按钮样式
	$("button.btn.btn-sm:gt(0)").attr("style", "margin:-5px 0px 0px 4px");

	// 3.7 分页样式
	$(".dataTables_paginate.paging_bootstrap li>a i").parent().attr("style", "height:32px");

};

//Table 的参数设置， 其中，参数 aoColumns  ，通过变量 dataTables_aoColumns 传入！
/*jQuery(function($) {
	// 3.1 table表头上的样式
	
	// 初始化table表头，确认表头元素是否有排序功能
	var dataTable1Obj = $('#data-table-1').dataTable({
		//参数1，列排序
		"aoColumns" : dataTables_aoColumns,
		// 参数2，表格中文提示
		oLanguage : {
			"sProcessing" : "正在加载中......",
			"sLengthMenu" : "每页显示 _MENU_ 条记录",
			"sZeroRecords" : "对不起，查询不到相关数据！",
			"sEmptyTable" : "未检索到相关数据！",
			"sInfo" : "当前显示： _START_ 到 _END_ 条，共 _TOTAL_ 条",
			"sInfoFiltered" : "数据表中共有 _MAX_ 条记录",
			"sSearch" : "",
			"oPaginate" : {
				"sFirst" : "首页",
				"sPrevious" : "上一页",
				"sNext" : "下一页",
				"sLast" : "末页",
				"sSelectPage":"跳转"
			}
		},
		// 其余参数
		bLengthChange : false,
		bRetrieve : true

	});*/

	// 初始化checkbox列，点击表头的checkbox能全选
	$('table th input:checkbox').on('click', function() {
			var that = this;
			$(this).closest('table').find('tr > td:first-child input:checkbox').each(
					function() {
						this.checked = that.checked;
						$(this).closest('tr').toggleClass('selected');
					}
			);
		}
	);
	

