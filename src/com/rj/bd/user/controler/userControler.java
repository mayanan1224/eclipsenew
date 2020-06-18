package com.rj.bd.user.controler;

import java.util.List;

import com.hbbc.util.hf.HFContext;
import com.rj.bd.user.entity.user;
import com.rj.bd.util.DBUtil;


/**
 * @desc  控制台
 * @author 楠木清澄
 *
 */
public class userControler {
	public int id;
	//实体类
	public List<user> users;
	
	//后端url路径
	public String formAction;
	//页面数
	public Integer totalPages=0;//总页数
	public Integer totalElements=0;//总记录数
	public Integer currentPage=0;//第几页

	
	//相当于servlet
	public void index(HFContext hfc) throws Exception{
		//前台传到后台
		hfc.input.fillVO(this);
		//查询表中所有字段----前期准备
		StringBuffer selectAll = new StringBuffer("select * from user");
		//表字段的的数量-----前期准备
		StringBuffer num = new StringBuffer("SELECT count(1) FROM user");
		//下表为零的是第一页
		if(currentPage==0){
			currentPage=1;
		}
		
//	------------	开始查找数据-------------
		//表字段的的数量
		String count=DBUtil.getInstance().queryReturnSingleString(num.toString());
	    //总记录数
		totalElements=Integer.valueOf(count);
		//总页数
		totalPages=totalElements%10==0?totalElements/10:totalElements/10+1;
		
		
		//查询当前页面的数据
		selectAll.append("ORDER bY id desc limit "+10*(currentPage+1)+",10");
		users=(List<user>) DBUtil.getInstance().query(selectAll.toString());
		
		//转向---后端传给前端
		this.formAction="com.rj.bd.user.userControler.index.hf?";
		hfc.output.forward(this, "user/list.jsp");
	}
	

}
