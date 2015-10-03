$ ->
    # init materialize components
    $(".button-collapse").sideNav()
    #$('.modal-trigger').leanModal()

    $('#deliver_now').click ->
        $('#feed_list').empty()
        $('#feed_list').show()
        $('#empty_div').remove()
        subscriptions = $('#subscriptions').find('tr')
        subscriptionsLength = subscriptions.length
        for i in [0...subscriptionsLength]
            subscription = subscriptions.eq(i)
            columns = subscription.find('td')
            if columns.length > 0
                checkbox = columns.eq(0).find('.filled-in')
                if checkbox.is(':checked')
                    feed_id = checkbox.attr('name')
                    title = columns.eq(1).text()
                    $('#feed_list')
                        .append("<li class='collection-item'>
                                    <div feed_id='#{feed_id}' title='#{title}'>
                                        #{title}
                                        <a href='#!' class='secondary-content self_remove'>
                                            <i class='material-icons'>clear</i>
                                        </a>
                                    </div>
                                </li>")
        if $('#feed_list').children().length == 0
            alert('nothing to deliver')
        else
            $('#modal1').openModal();
   
    $(document).on 'click', '.self_remove', (event) ->
        $(@).parent().parent().remove()
        if $('#feed_list').children().length == 0
            $('#feed_list').parent().append('<div id="empty_div">empty</div>') #todo nice message
            $('#feed_list').hide()
            # todo disable deliver button

    $('#deliver_button').click ->
        $('#progress').show()
        form = $('#deliver_form')
        mode = form.find('#mode')
        request = new Object()
        if mode.is(":checked")
        # todo
        else
            includeImages = form.find('#include_images')
            markAsRead = form.find('#mark_as_read')
            fullArticle = form.find('#full')
            feeds = form.find("#feed_list").children()
            feedsLength = feeds.length
            request.feeds = []
            for i in [0...feedsLength]
                feed = feeds.eq(i).children().eq(0)
                feedRequest = new Object()
                feedRequest.id = feed.attr('feed_id')
                feedRequest.title = feed.attr('title')
                feedRequest.includeImages = includeImages.is(":checked")
                feedRequest.fullArticle = fullArticle.is(":checked")
                feedRequest.markAsRead = markAsRead.is(":checked")
                request.feeds.push(feedRequest)

        $.ajax
            url: 'deliver',
            type: 'POST',
            data: JSON.stringify(request),
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: ->
                $('#progress').hide()
                alert('success')
            error: ->
                $('#progress').hide()
                alert('error')

    $("#search_box").keyup ->
        filter = $(this).val()
        subscriptions = $('#subscriptions').find('tr')
        subscriptionsLength = subscriptions.length
        for i in [0...subscriptionsLength]
            subscription = subscriptions.eq(i)
            columns = subscription.find('td')
            if columns.length > 0
                text = columns.eq(1).text()
                if text.search(new RegExp(filter, "i")) < 0
                    subscription.hide()
                else
                    subscription.show();