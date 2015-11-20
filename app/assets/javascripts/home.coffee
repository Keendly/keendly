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
                                        <input type='checkbox' class='filled-in' id='include_images#{i}'/>
                                        <label for='include_images#{i}'>Include images</label>
                                        </p>
                                        <p>
                                        <input type='checkbox' class='filled-in' id='mark_as_read#{i}' checked='checked'/>
                                        <label for='mark_as_read#{i}'>Mark as read</label>
                                        </p>
                                        <p>
                                        <input type='checkbox' class='filled-in' id='full_article#{i}' checked='checked'/>
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

loadScheduleModal = (detailed)->
    if detailed
      includeImages = produceCheckbox('schedule_include_images', 'Include articles images', 'Many images may slow down ebook downloading')
      markAsRead = produceCheckbox('schedule_mark_as_read', 'Mark feed as read', null)
      markAsRead = produceCheckbox('schedule_full', 'Deliver full articles', 'Extract article text from webpage. Not needed if feed already contains full article text.')
      time = produceTimes()
      console.log(detailed)
    else
      console.log(detailed)

produceCheckbox = (id, text, tooltip) ->
    input = "<input type='checkbox' class='filled-in' id='#{id}'/>
              <label for='#{id}>#{text}</label>"
    if tooltip != null
        input = input +
              "<i data-position='right' data-delay='50' data-tooltip='#{tooltip}' class='tooltipped tiny material-icons hide-on-med-and-down'>info_outline</i>"
    return "<p>" + input + "</p"

produceTimes = ->
    "<p>
    <label>What time you want your feed to be delivered?</label>
    <select id='times' class='browser-default'>
        <option>12:00 AM</option>
        <option>01:00 AM</option>
        <option>02:00 AM</option>
        <option>03:00 AM</option>
        <option>04:00 AM</option>
        <option>05:00 AM</option>
        <option>06:00 AM</option>
        <option>07:00 AM</option>
        <option>08:00 AM</option>
        <option>09:00 AM</option>
        <option>10:00 AM</option>
        <option>11:00 AM</option>
        <option>12:00 PM</option>
        <option>01:00 PM</option>
        <option>02:00 PM</option>
        <option>03:00 PM</option>
        <option>04:00 PM</option>
        <option>05:00 PM</option>
        <option>06:00 PM</option>
        <option>07:00 PM</option>
        <option>08:00 PM</option>
        <option>09:00 PM</option>
        <option>10:00 PM</option>
        <option>11:00 PM</option>
    </select>
</p>"
