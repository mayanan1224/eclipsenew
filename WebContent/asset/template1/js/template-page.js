
// ---------------------------------------------------------服务器端分页-------------------------------------------------------------------------
function ACETemplate1Adjust() {
	$("#data-table-1>thead tr>th").attr("style", "font-weight:bold;color:black;text-align:center;font-size:13px");

	// 3.2 table搜索样式
	//$("#data-table-1_filter label input").attr("placeholder", "搜索(多条件搜索中间用空格隔开)").attr("style", "width:50%");
	$("#data-table-1_filter label input").attr("style", "display:none;width:50%");
	
	$(".row:before").attr("style","display:none !important");
	$(".row:after").attr("style","display:none !important")
	
	$("#data-table-1_filter label").attr("style", "width:100%");
	$("#data-table-1_filter").attr("style", "margin-right: -16px;");
	
	$("#data-table-1_wrapper .row").first().children().eq(1).attr("style", "margin-top:10px");
	$("#data-table-1_wrapper").attr("style", "margin-top:-30px");

	// 3.3 增加表单行间距样式
	$("div .modal-body .row:not(#iconr)").attr("style", "margin-top:-14px;");
	$("div .modal-body .row .col-sm-3").attr("style", "margin-top:6px;text-align:right");
	$("div#myModal .modal-body .row i:lt(3)").attr("style", "margin-top:9px;text-align:right;color:gray");

	// 3.6 按钮样式
	$("button.btn.btn-sm:gt(0)").attr("style", "margin:-5px 0px 0px 4px");

	// 3.7 分页样式
	$(".dataTables_paginate.paging_bootstrap li>a i").parent().attr("style", "height:32px");

};
jQuery(function($) {
	// 初始化表格
	var dataTable1Obj = $('#data-table-1').DataTable($.dataTablesSettings);
	 $('#searchButton').click(function () {
         //这里重新设置参数
         $.dataTablesSettings.fnServerParams = function (aoData) {
             aoData._rand = Math.random();
             // 添加额外参数
            /* aoData.push(
                 { "name": "year", "value": $('#year').val() },
                 { "name": "month", "value": $('#month').val() },
                 { "name": "StartTime", "value": $('#StartTime').val() },
                 { "name": "EndTime", "value": $('#EndTime').val() },
                 { "name": "DTMName", "value": $('#DTMName').val() },
                 { "name": "KeyWords", "value": $('#KeyWords').val() }
                 );*/
         }
         //搜索就是设置参数，然后销毁datatable重新再建一个
         dataTable1Obj.fnClearTable(); //清空一下table
         dataTable1Obj = $("#data-table-1").DataTable($.dataTablesSettings);
         //搜索后跳转到第一页
         dataTable1Obj.fnPageChange(0);
     });

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
	
});

//函数的参数是固定，不能更改。  
function retrieveData( sSource, aoData, fnCallback ) {   
    $.ajax( {     
        type: "POST",      
        //contentType: "application/json",   //这段代码不要加，我时延的是否后台会接受不到数据  
        // 利用 ？ 传值将将搜索表单内参数传递（id 为 searchFrom）
        url: sSource + "?" + $("#searchFrom").serialize(),   
        dataType:"json",  
        data:aoData,
        success: function(data) {   
            fnCallback(data); //服务器端返回的对象的returnObject部分是要求的格式     
        }     
    });    
}   


$(function() {
	$(".ace-cuitooltip").tooltip({
		animation : true,
		trigger : 'hover'
	});
});


