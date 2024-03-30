function like(btn, entityType, entityId, entityUserId) { //html中传入的参数
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) { //请求成功
            //改变按钮btn的下级标签i和b（见discuss-detail页面中），增量式更新页面
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            } else {
                alert(data.msg); //失败了直接弹出提示
            }
        }
    );
}