$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();
	$.post( //异步的发post请求
	    CONTEXT_PATH + "/letter/send",
	    {"toName":toName,"content":content},
	    function(data) { //处理服务器返回的data对象
	        data = $.parseJSON(data); //人为地转换为JSON对象
	        if(data.code == 0) {
	            $("#hintBody").text("发送成功!"); //发送到html中的提示框（id为#hintbody,在letter-detail.html中）
	        } else {
	            $("#hintBody").text(data.msg);
	        }

	        $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");//2s后隐藏提示框
                location.reload();//2s后从服务器重载当前页面
            }, 2000);
	    }
	);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}