$ ->
    # init materialize components
    $(".button-collapse").sideNav()

    presetTimezone()
    $('#deliver_now').click ->
        feedList = $('#feed_list')
        clearFeedList(feedList)
        fillWithSelectedSubscriptions(feedList, false)
        if feedList.children().length == 0
            alert('nothing to deliver')
        else
            $('#deliver_modal').openModal();

    $('#schedule_delivery').click ->
        feedList = $('#schedule_feed_list')
        clearFeedList(feedList)
        fillWithSelectedSubscriptions(feedList, false)
        if feedList.children().length == 0
            alert('nothing to deliver')
        else
            $('#schedule_modal').openModal();

    $(document).on 'click', '.self_remove', (event) ->
        list = $(@).parent().parent().parent()
        if list.children().length == 1
            list.parent().append('<div class="empty_div">empty</div>') #todo nice message
            list.hide()
            # todo disable deliver button
        $(@).parent().parent().remove()

    $('#schedule_button').click ->
        $('#deliver_progress').show()
        form = $('#schedule_form')
        mode = form.find('#schedule_mode')
        request = new Object()
        if mode.is(":checked")
            feeds = form.find("#detailed_schedule_feed_list").children()
            feedsLength = feeds.length
            request.feeds = []
            for i in [0...feedsLength]
                feed = feeds.eq(i).children().eq(0)
                feedRequest = new Object()
                feedRequest.id = feed.attr('feed_id')
                feedRequest.title = feed.attr('title')
                feedRequest.includeImages = feed.find('.include_images').is(":checked")
                feedRequest.fullArticle = feed.find('.full_article').is(":checked")
                feedRequest.markAsRead = feed.find('.mark_as_read').is(":checked")
                request.feeds.push(feedRequest)
        else
            includeImages = form.find('#schedule_include_images')
            markAsRead = form.find('#schedule_mark_as_read')
            fullArticle = form.find('#schedule_full')
            feeds = form.find("#schedule_feed_list").children()
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
                $('#deliver_progress').hide()
                alert('success')
            error: ->
                $('#deliver_progress').hide()
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

    $('#mode').change ->
        feedList = $('#feed_list')
        detailedFeedList = $('#detailed_feed_list')
        checked = $('#mode').is(':checked')
        if checked
          clearFeedList(detailedFeedList)
          fillWithSelectedSubscriptions(detailedFeedList, true)
          $('#detailed').show()
          $('#simple').hide()
        else
          clearFeedList(feedList)
          fillWithSelectedSubscriptions(feedList, false)
          $('#detailed').hide()
          $('#simple').show()

    $('#schedule_mode').change ->
        feedList = $('#schedule_feed_list')
        detailedFeedList = $('#detailed_schedule_feed_list')
        checked = $('#schedule_mode').is(':checked')
        if checked
          clearFeedList(detailedFeedList)
          fillWithSelectedSubscriptions(detailedFeedList, true)
          $('#schedule_detailed').show()
          $('#schedule_simple').hide()
        else
          clearFeedList(feedList)
          fillWithSelectedSubscriptions(feedList, false)
          $('#schedule_detailed').hide()
          $('#schedule_simple').show()

fillWithSelectedSubscriptions = (elementToFill, detailed) ->
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
                if detailed
                  elementToFill
                      .append("<li class='collection-item'>
                                  <div feed_id='#{feed_id}' title='#{title}'>
                                      #{title}
                                      <a href='#!' class='secondary-content self_remove'>
                                          <i class='material-icons'>clear</i>
                                      </a>
                                      <p>
                                        <input type='checkbox' class='filled-in include_images' id='include_images#{i}'/>
                                        <label for='include_images#{i}'>Include images</label>
                                        </p>
                                        <p>
                                        <input type='checkbox' class='filled-in mark_as_read' id='mark_as_read#{i}' checked='checked'/>
                                        <label for='mark_as_read#{i}'>Mark as read</label>
                                        </p>
                                        <p>
                                        <input type='checkbox' class='filled-in full_article' id='full_article#{i}' checked='checked'/>
                                        <label for='full_article#{i}'>Full article</label>
                                      </p>
                                      </div></li>")
                else
                  elementToFill
                      .append("<li class='collection-item'>
                                  <div feed_id='#{feed_id}' title='#{title}'>
                                      #{title}
                                      <a href='#!' class='secondary-content self_remove'>
                                          <i class='material-icons'>clear</i>
                                      </a>
                                      </div></li>")

clearFeedList = (list) ->
    list.empty()
    list.show()
    $('.empty_div').remove()

presetTimezone = ->
    tz = jstz.determine();
    timezones = $('#timezones').find('option')
    for i in [0...timezones.length]
        timezone = timezones[i]
        if timezone.value == tz.name()
            timezone.selected = "selected"
