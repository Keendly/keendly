var FeedBox = React.createClass({
  loadFeeds: function() {
    $.ajax({
      url: this.props.url,
      dataType: 'json',
      cache: false,
      success: function(data) {
        this.setState({data: data});
      }.bind(this),
      error: function(xhr, status, err) {
        window.location.replace("login");
      }.bind(this)
    });
  },
  getInitialState: function() {
    return {data: []};
  },
  componentDidMount: function() {
    this.loadFeeds();
  },
  deliverButtonClick: function() {
    var timestamp = Date.now()
    ReactDOM.render(
      <DeliverModal url='api/deliveries' key={timestamp} success={this.handleDeliverySuccess} error={this.handleDeliveryError}/>,
      document.getElementById('modal')
    );
    $('#delivery_modal').openModal()
  },
  subscribeButtonClick: function() {
    var timestamp = Date.now()
    ReactDOM.render(
      <SubscribeModal url='api/subscriptions' key={timestamp} success={this.handleSubscriptionSuccess} error={this.handleSubscriptionError}/>,
      document.getElementById('modal')
    );
    $('#subscription_modal').openModal()
  },
  handleDeliverySuccess: function() {
    this.setState({deliverySuccess: true, error: false})
  },
  handleDeliveryError: function(code, description) {
     this.setState({deliverySuccess: false, subscriptionSuccess: false, error: true, errorCode: code, errorDescription: description})
  },
  handleSubscriptionSuccess: function() {
    this.setState({subscriptionSuccess: true, error: false})
    this.loadFeeds()
  },
  handleSubscriptionError: function(code, description) {
     this.setState({deliverySuccess: false, subscriptionSuccess: false, error: true, errorCode: code, errorDescription: description})
  },
  render: function() {
    return (
      <div className="container" id="subs-container">
        {
          this.state.error == true ?
            (this.state.errorCode == 'DELIVERY_EMAIL_NOT_CONFIGURED' ?
            <div className='error_div'>
              <b>Send-to-Kindle</b> email address not set, configure it in <a href="/user">settings</a>.
            </div> :
            <div className='error_div'>
              {this.state.errorDescription}
            </div>)
              : ''}
        {this.state.deliverySuccess == true ? <div className='success_div'>Delivery started :-) Give us few minutes to deliver articles to your Kindle.</div> : ''}
        {this.state.subscriptionSuccess == true ? <div className='success_div'>Subscription saved :-> your articles will be now delivered automatically.</div> : ''}
        <div className="row" id="button_row">
          <div className="col s12 m6">
            <a onClick={this.deliverButtonClick} className="waves-effect waves-light btn modal-trigger" id="delivery_modal_btn" href="#delivery_modal">Deliver now</a>
            <a onClick={this.subscribeButtonClick} className="waves-effect waves-light btn modal-trigger" id="subscription_modal_btn" href="#subscription_modal">Subscribe</a>
          </div>
          <div className="input-field col offset-m3 s12 m3" id="search">
            <input id="search_box" type="search" required />
            <label htmlFor="search_box"><i className="material-icons">search</i></label>
          </div>
        </div>
        <FeedList data={this.state.data} />
      </div>
    );
  }
});

var FeedList = React.createClass({
  render: function() {
  var feedNodes = this.props.data.map(function(feed) {
    return (
      <Feed title={feed.title} key={feed.feedId} feedId={feed.feedId} lastDelivery={feed.lastDelivery} subscriptions={feed.subscriptions} />
    );
    });
    return (
    <form>
      <table className="highlight" id="subscriptions">
        <thead>
        <tr>
          <th></th>
          <th>Title</th>
          <th>Last delivery</th>
          <th>Next delivery</th>
        </tr>
        </thead>

        <tbody>
          {feedNodes}
        </tbody>
      </table>
      </form>
    );
  }
});

var DeliverModal = React.createClass({
  getInitialState: function() {
    return {mode: 'simple', feeds: this.getSelectedFeeds()};
  },
  componentDidMount: function() {
      var feedIds = []
      $.each(this.state.feeds, function(index, value) {
          feedIds.push(value.feedId)
      });
      $.ajax({
         url: 'api/feeds/unreadCount',
         type: "POST",
         data: JSON.stringify(feedIds),
         contentType: "application/json; charset=utf-8",
         success: function(data) {
            var newFeeds = []
            $.each(this.state.feeds, function(index, value) {
                var found = false;
                $.each(data, function(feedId, unread) {
                  if (feedId == value.feedId) {
                    newFeeds.push({'title': value.title, 'feedId': value.feedId, 'unread': unread})
                    found = true;
                  }
                });
                if (!found){
                  newFeeds.push({'title': value.title, 'feedId': value.feedId})
                }
            });
            this.setState({
              feeds: newFeeds
            });
            $('.tooltipped').tooltip({delay: 50});
         }.bind(this)
      });
  },
  modeChangeClick: function(event) {
    this.setState({
      'mode': event.target.checked ? 'detailed' : 'simple'
    });
  },
  checkDeliveryState: function(id, count) {
    if (count > 24){
      return; // 2 minutes
    }
    $.ajax({
      url: "api/deliveries/" + id,
      dataType: 'json',
      cache: false,
      global: false,
      success: function(data) {
        if (data.deliveryDate != null){
           Materialize.toast('Articles delivered!', 4000, 'pink')
        } else if (data.error != null){
           // error, stop polling
        } else {
          setTimeout( this.checkDeliveryState.bind(null, data.id, count+1), 5000);
        }
      }.bind(this)
    });
  },
  handleSubmit: function() {
     if (this.state.mode == 'detailed'){
         $.each( this.state.feeds, function( i, feed ) {
           feed['includeImages'] = $(document.getElementById(feed.feedId + 'img')).is(':checked');
           feed['fullArticle'] =  $(document.getElementById(feed.feedId + 'full')).is(':checked');
           feed['markAsRead'] = $(document.getElementById(feed.feedId + 'mark_as_read')).is(':checked');
         });
     } else {
        $.each( this.state.feeds, function( i, feed ) {
          feed['includeImages'] = $('#include_images').is(':checked');
          feed['fullArticle'] =  $('#full_article').is(':checked');
          feed['markAsRead'] = $('#mark_as_read').is(':checked');
        });
     }
     $.ajax({
       url: this.props.url,
       type: "POST",
       data: JSON.stringify({'manual': true, 'items': this.state.feeds}, ["items", "title", "feedId", "includeImages", "fullArticle", "markAsRead", "manual"]),
       contentType: "application/json; charset=utf-8",
       success: function(data) {
         $('#delivery_modal').closeModal();
         this.props.success()
         setTimeout( this.checkDeliveryState.bind(null, data.id, 1), 5000);
       }.bind(this),
       error: function(xhr, status, err) {
         $('#delivery_modal').closeModal();
         this.props.error(xhr.responseJSON.code, xhr.responseJSON.description)
       }.bind(this)
     });
  },
  getSelectedFeeds: function() {
    var checkbox, columns, feed_id, i, j, ref, results, subscription, subscriptions, subscriptionsLength, title;
    subscriptions = $('#subscriptions').find('tr');
    subscriptionsLength = subscriptions.length;
    results = [];
    for (i = j = 0, ref = subscriptionsLength; 0 <= ref ? j < ref : j > ref; i = 0 <= ref ? ++j : --j) {
      subscription = subscriptions.eq(i);
      columns = subscription.find('td');
      if (columns.length > 0) {
        checkbox = columns.eq(0).find('.filled-in');
        if (checkbox.is(':checked')) {
          feed_id = checkbox.attr('id');
          results.push({'title': columns.eq(1).text(), 'feedId': feed_id});
        }
      }
    }
    return results;
  },
  render: function() {
    var mode = this.state.mode;
    var feeds = this.state.feeds;
    if (feeds.length == 0){
      return (
          <div id="delivery_modal" className="modal">
            <div className="error_modal">
              Select feeds first
              </div>
          </div>
        )
    }

    var list
    if (mode == 'simple'){
      var actualList = feeds.map(function(feed) {
          return (
            <SelectedFeed_Simple key={feed.feedId} feed={feed} />
          );
        });
      list =
      <div id="simple">
       <p>
         <input type="checkbox" className="filled-in" id="include_images" defaultChecked/>
         <label htmlFor="include_images">Include images</label>
       </p>
       <p>
         <input type="checkbox" className="filled-in" id="full_article" defaultChecked/>
         <label htmlFor="full_article">Extract article</label>
       </p>
        <p>
          <input type="checkbox" className="filled-in" id="mark_as_read" defaultChecked/>
          <label htmlFor="mark_as_read">Mark as read</label>
        </p>
       <ul className="collection" id="feed_list">
          {actualList}
       </ul>
      </div>
     } else {
       var actualList = feeds.map(function(feed) {
           return (
              <SelectedFeed_Detailed feed={feed} key={feed.feedId}/>
           );
         });
       list =
       <div id="detailed">
           <ul className="collection" id="detailed_feed_list">
            {actualList}
           </ul>
       </div>
     }
    return (
      <div id="delivery_modal" className="modal">
          <div className="modal-content" id="delivery_form">
              <h4>Deliver feeds</h4>
              <ModeSwitch onChange={this.modeChangeClick} mode={mode} />
              {list}
          </div>
          <div className="modal-footer">
              <a href="#!" className="modal-action modal-close waves-effect waves-red btn-flat">Cancel</a>
              <a href="#!" onClick={this.handleSubmit} className="modal-action waves-effect waves-green btn-flat submit save" id="delivery_save_btn">Deliver</a>
          </div>
      </div>
    );
  }
});

var Feed = React.createClass({
  render: function() {
    if (this.props.subscriptions){
      var scheduled = [];
      var subscriptionsLength = this.props.subscriptions.length;
      var now = moment().toDate().getTime();
      for (var i = 0; i < subscriptionsLength; i++){
        var s = this.props.subscriptions[i]
        var tz = s['timezone']

        var nowInTz = moment(now).tz(tz);
        if (nowInTz.format('HH:mm') > s['time']){
          var nextScheduledDelivery = nowInTz.add(1, 'd').format('YYYY-MM-DD') + ' ' + s.time
          // next tomorrow
        } else {
          var nextScheduledDelivery = nowInTz.format('YYYY-MM-DD') + ' ' + s.time
          // today
        }
        var nextDelivery = moment.tz(nextScheduledDelivery, tz).toDate().getTime()
        scheduled.push(nextDelivery)
      }
      var soonestNextScheduledDelivery = Math.min.apply(Math, scheduled)
      var nextDeliveryRelative = moment(now).to(moment(soonestNextScheduledDelivery))
    }

    return (
      <tr>
        <td>
          <input type="checkbox" className="filled-in" id={this.props.feedId} />
          <label htmlFor={this.props.feedId}></label>
        </td>
        <td className="feed_title">{this.props.title}</td>
        <td>{this.props.lastDelivery != null ? moment(this.props.lastDelivery.deliveryDate).fromNow() : ''}</td>
        <td>{nextDeliveryRelative}</td>
      </tr>
    );
  }
});

var SubscribeModal = React.createClass({
  getInitialState: function() {
    return {mode: 'simple', feeds: this.getSelectedFeeds()};
  },
  modeChangeClick: function(event) {
    this.setState({
      'mode': event.target.checked ? 'detailed' : 'simple'
    });
  },
  handleSubmit: function() {
     this.state.time = $('#time').val()
     this.state.timezone = $('#timezone').val()
     if (this.state.mode == 'detailed'){
         $.each( this.state.feeds, function( i, feed ) {
           feed['includeImages'] = $(document.getElementById(feed.feedId + 'img')).is(':checked');
           feed['fullArticle'] =  $(document.getElementById(feed.feedId + 'full')).is(':checked');
           feed['markAsRead'] = $(document.getElementById(feed.feedId + 'mark_as_read')).is(':checked');
         });
     } else {
        $.each( this.state.feeds, function( i, feed ) {
          feed['includeImages'] = $('#include_images').is(':checked');
          feed['fullArticle'] =  $('#full_article').is(':checked');
          feed['markAsRead'] = $('#mark_as_read').is(':checked');
        });
     }
     $.ajax({
       url: this.props.url,
       type: "POST",
       data: JSON.stringify({
          'time': this.state.time,
          'timezone': this.state.timezone,
          'feeds': this.state.feeds
       }, ["feeds", "feedId", "includeImages", "fullArticle", "markAsRead", "time", "timezone", "title"]),
       contentType: "application/json; charset=utf-8",
       success: function(data) {
         $('#subscription_modal').closeModal();
         this.props.success()
       }.bind(this),
       error: function(xhr, status, err) {
         $('#subscription_modal').closeModal();
         this.props.error(xhr.responseJSON.code, xhr.responseJSON.description)
       }.bind(this)
     });
  },
  getSelectedFeeds: function() {
    var checkbox, columns, feed_id, i, j, ref, results, subscription, subscriptions, subscriptionsLength, title;
    subscriptions = $('#subscriptions').find('tr');
    subscriptionsLength = subscriptions.length;
    results = [];
    for (i = j = 0, ref = subscriptionsLength; 0 <= ref ? j < ref : j > ref; i = 0 <= ref ? ++j : --j) {
      subscription = subscriptions.eq(i);
      columns = subscription.find('td');
      if (columns.length > 0) {
        checkbox = columns.eq(0).find('.filled-in');
        if (checkbox.is(':checked')) {
          feed_id = checkbox.attr('id');
          results.push({'title': columns.eq(1).text(), 'feedId': feed_id});
        }
      }
    }
    return results;
  },
  render: function() {
    var mode = this.state.mode;
    var feeds = this.state.feeds;
    if (feeds.length == 0){
      return (
          <div id="subscription_modal" className="modal">
            <div className="error_modal">
              Select feeds first
              </div>
          </div>
        )
    }

    var list
    if (mode == 'simple'){
      var actualList = feeds.map(function(feed) {
          return (
            <SelectedFeed_Simple key={feed.feedId} feed={feed} />
          );
        });
      list =
      <div id="simple">
       <p>
         <input type="checkbox" className="filled-in" id="include_images" defaultChecked/>
         <label htmlFor="include_images">Include images</label>
       </p>
       <p>
         <input type="checkbox" className="filled-in" id="full_article" defaultChecked/>
         <label htmlFor="full_article">Full article</label>
       </p>
        <p>
          <input type="checkbox" className="filled-in" id="mark_as_read" defaultChecked/>
          <label htmlFor="mark_as_read">Mark as read</label>
        </p>
          <Time />
          <TimeZone />
       <ul className="collection" id="feed_list">
          {actualList}
       </ul>
      </div>
     } else {
       var actualList = feeds.map(function(feed) {
           return (
              <SelectedFeed_Detailed feed={feed} key={feed.feedId}/>
           );
         });
       list =
       <div id="detailed">
           <Time />
           <TimeZone />
           <ul className="collection" id="detailed_feed_list">
            {actualList}
           </ul>
       </div>
     }
    return (
      <div id="subscription_modal" className="modal">
          <div className="modal-content" id="delivery_form">
              <h4>Subscribe to feeds</h4>
              <ModeSwitch onChange={this.modeChangeClick} mode={mode} />
              {list}
          </div>
          <div className="modal-footer">
              <a href="#!" className="modal-action modal-close waves-effect waves-red btn-flat">Cancel</a>
              <a href="#!" onClick={this.handleSubmit} className="modal-action waves-effect waves-green btn-flat submit save" id="subscription_save_btn">Subscribe</a>
          </div>
      </div>
    );
  }
});

var Time = React.createClass({
  render: function(){
    var options = [];
    var current = moment().format('HH:mm');

    for (var i=0; i < 24; i++) {
        var hour = i
        if (hour < 10){
          hour = '0' + hour
        }

        options.push(<option key={i + ':00'} value={hour + ':00'}>{hour}:00</option>)
        options.push(<option key={i + ':30'} value={hour + ':30'}>{hour}:30</option>)
    }
    return (
      <p>
      <label htmlFor="time">Delivery time</label>
          <select id="time" className="browser-default times" defaultValue={moment().format('HH:00')}>
           {options}
          </select>
      </p>
    )
  }
});

var TimeZone = React.createClass({
  render: function(){
    var options = [];
    var timezones = moment.tz.names()
    var arrayLength = timezones.length;

    var guessed = moment.tz.guess();
    for (var i = 0; i < arrayLength; i++) {
      var tz = timezones[i]
      options.push(<option value={tz} key={tz}>{tz}</option>)
    }
    return (
      <p>
      <label htmlFor="timezone">Time zone</label>
          <select id="timezone" className="browser-default times" defaultValue={guessed}>
           {options}
          </select>

      </p>
    )
  }
});

var SelectedFeed_Simple = React.createClass({
  render: function(){
    var feed = this.props.feed;
    return (
      <li className='collection-item'>
        <div feed_id={feed.feedId} title={feed.title}>
        {feed.title}
        {function(){
          if (typeof feed.unread !== 'undefined'){
            return <span className="new badge">{feed.unread}</span>
          }
        }.call(this)
        }
        {function(){
          if (feed.unread > 100) {
            return <i className="tiny material-icons error-icon tooltipped" data-position="left" data-delay="50" data-tooltip="Only 100 will be delivered">error_outline</i>
          }
        }.call(this)
        }
        </div>
      </li>
    )
  }
});

var SelectedFeed_Detailed = React.createClass({
  render: function(){
    var feed = this.props.feed;
    return (
      <li className='collection-item' key={feed.feedId}>
       <div feed_id={feed.feedId} id={feed.feedId} title={feed.title}>
           {feed.title}
           {function(){
             if (typeof feed.unread !== 'undefined'){
               return <span className="new badge">{feed.unread}</span>
             }
           }.call(this)
           }
         <p>
           <input type="checkbox" id={feed.feedId + 'img'} className="filled-in" defaultChecked/>
           <label htmlFor={feed.feedId + 'img'}>Include images</label>
         </p>
         <p>
           <input type="checkbox" id={feed.feedId + 'full'} className="filled-in" defaultChecked/>
           <label htmlFor={feed.feedId + 'full'}>Extract article</label>
         </p>
        <p>
          <input type="checkbox" id={feed.feedId + 'mark_as_read'} className="filled-in" defaultChecked/>
          <label htmlFor={feed.feedId + 'mark_as_read'}>Mark as read</label>
        </p>
       </div>
      </li>
    )
  }
});

var ModeSwitch = React.createClass({
  render: function(){
    var mode = this.props.mode;
    var checkbox = mode == 'simple' ?
      <input onChange={this.props.onChange} type="checkbox" id="delivery_mode"/> :
      <input onChange={this.props.onChange} type="checkbox" id="delivery_mode" checked="checked" />
    return (
      <div className="switch right-align">
         <label>
             Simple
             {checkbox}
             <span className="lever"></span>
             Detailed
         </label>
      </div>
    )
  }
});

ReactDOM.render(
  <FeedBox url="api/feeds" />,
  document.getElementById('content')
);

$(document).ready(function(){
  $("#search_box").keyup(function() {
    var columns, filter, i, j, ref, results, subscription, subscriptions, subscriptionsLength, text;
    filter = $(this).val();
    subscriptions = $('#subscriptions').find('tr');
    subscriptionsLength = subscriptions.length;
    results = [];
    for (i = j = 0, ref = subscriptionsLength; 0 <= ref ? j < ref : j > ref; i = 0 <= ref ? ++j : --j) {
      subscription = subscriptions.eq(i);
      columns = subscription.find('td');
      if (columns.length > 0) {
        text = columns.eq(1).text();
        if (text.search(new RegExp(filter, "i")) < 0) {
          results.push(subscription.hide());
        } else {
          results.push(subscription.show());
        }
      } else {
        results.push(void 0);
      }
    }
    return results;
  });
});


