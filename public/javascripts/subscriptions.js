var DeliveryBox = React.createClass({
  loadSubscriptions: function(page) {
    $.ajax({
      url: this.props.url + '?page=' + page +'&pageSize=20',
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
    return {data: [], page: 1};
  },
  componentDidMount: function() {
    this.loadSubscriptions(this.state.page);
  },
  handlePageClick: function(newPage) {
    this.setState({
      page: newPage
    });
    this.loadSubscriptions(newPage);
  },
  render: function() {
    return (
      <div className="container">
        <SubscriptionList data={this.state.data} />
        <Pagination handleClick={this.handlePageClick} page={this.state.page} />
      </div>
    );
  }
});

var SubscriptionList = React.createClass({
  render: function() {
    var subscriptions = this.props.data.map(function(subscription) {
      return (
        <Subscription feeds={subscription.feeds} key={subscription.id} time={subscription.time} timezone={subscription.timezone} />
      );
    });
    return (
      <table id="subscriptions">
        <thead>
        <tr>
          <th>Feeds</th>
          <th>Delivery time</th>
        </tr>
        </thead>

        <tbody>
          {subscriptions}
        </tbody>
      </table>
    );
  }
});

var Subscription = React.createClass({
  render: function() {
    if (this.props.feeds != null){
      var feeds = this.props.feeds.map(function(feed) {
        return (
          feed.title
        );
      });
    }
    return (
      <tr>
        <td>
            {feeds != null ? feeds.join(" \u2022 ") : ''}
        </td>
        <td>
            {this.props.time} ({this.props.timezone})
        </td>
      </tr>
    );
  }
});

var Pagination = React.createClass({
  handleClick: function(page) {
    this.props.handleClick(page);
  },
  render: function() {
    var currentPage = this.props.page;
    var left = currentPage == 1 ?
                  <li className="disabled"><i className="material-icons">chevron_left</i></li> :
                  <li className="waves-effect"><i onClick={this.handleClick.bind(this, currentPage-1)} className="material-icons">chevron_left</i></li>

    var numbers = [0,1,2,3].map(function(i) {
      if (currentPage == 1){
        if (i == 0){
          return <PageNumber handleClick={this.handleClick} active='true' page={currentPage} key={i} />
        } else {
          return <PageNumber handleClick={this.handleClick}  active='false' page={i + currentPage} key={i} />
        }
      }
      if (currentPage == 2){
        if (i == 1){
          return <PageNumber handleClick={this.handleClick}  active='true' page={currentPage} key={i} />
        } else {
          return <PageNumber handleClick={this.handleClick}  active='false' page={i + currentPage - 1} key={i} />
        }
      }
      if (currentPage > 2){
        if (i == 2){
          return <PageNumber handleClick={this.handleClick}  active='true' page={currentPage} key={i} />
        } else {
          return <PageNumber handleClick={this.handleClick}  active='false' page={i + currentPage - 2} key={i} />
        }
      }
    }, this);
    var right = <li className="waves-effect"><i onClick={this.handleClick.bind(this, currentPage+1)} className="material-icons">chevron_right</i></li>
    return (
      <ul className="pagination">
        {left}
        {numbers}
        {right}
      </ul>
    );
  }
});

var PageNumber = React.createClass({
  handleClick: function(page) {
    this.props.handleClick(page);
  },
  render: function() {
    var active = this.props.active;
    var page = this.props.page;
    if (active == 'true'){
      return <li onClick={this.handleClick.bind(this, page)} className="active">{page}</li>
    } else {
      return <li onClick={this.handleClick.bind(this, page)} className="waves-effect">{page}</li>
    }
  }
})

ReactDOM.render(
  <DeliveryBox url="api/subscriptions" />,
  document.getElementById('content')
);
