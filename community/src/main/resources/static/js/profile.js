$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) { //判断该按钮的状态是'btn-secondary'还是'btn-info'（profile.html中有）
		// 关注TA
		$.post(
		    CONTEXT_PATH + "/follow",
		    {"entityType":3,"entityId":$(btn).prev().val()}, //个人主页，关注的是用户，所以"entityType":3。$(btn).prev().val()指获取按钮的上一个结点的值
		    function(data) {
		        data = $.parseJSON(data);
		        if(data.code == 0) {
                    window.location.reload();//成功后直接刷新页面，少写点前端代码（可以通过前端代码来增量式更新，但我们重点不在此）
		        } else {
                    alert(data.msg); //返回错误信息
		        }
		    }
		);
	} else {
		// 取消关注
		$.post(
		    CONTEXT_PATH + "/unfollow",
		    {"entityType":3,"entityId":$(btn).prev().val()},
		    function(data) {
		        data = $.parseJSON(data);
		        if(data.code == 0) {
                    window.location.reload();
		        } else {
                    alert(data.msg);
		        }
		    }
		);
	}
}