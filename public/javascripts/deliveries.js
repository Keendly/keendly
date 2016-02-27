var DeliveryBox = React.createClass({
  loadFeeds: function(page) {
    $.ajax({
      url: this.props.url + '?page=' + page +'&pageSize=20',
      dataType: 'json',
      cache: false,
      success: function(data) {
        this.setState({data: data});
      }.bind(this),
      error: function(xhr, status, err) {
        window.location.replace("login?error=Log in first");
      }.bind(this)
    });
  },
  getInitialState: function() {
    return {data: [], page: 1};
  },
  componentDidMount: function() {
    this.loadFeeds(this.state.page);
  },
  handlePageClick: function(newPage) {
    this.setState({
      page: newPage
    });
    this.loadFeeds(newPage);
  },
  render: function() {
    return (
      <div className="container">
        <DeliveryList data={this.state.data} />
        <Pagination handleClick={this.handlePageClick} page={this.state.page} />
      </div>
    );
  }
});

var DeliveryList = React.createClass({
  render: function() {
    var deliveryNodes = this.props.data.map(function(delivery) {
      return (
        <Delivery items={delivery.items} key={delivery.id} date={delivery.deliveryDate} />
      );
    });
    return (
      <table id="deliveries">
        <thead>
        <tr>
          <th>Feeds</th>
          <th>Delivery date</th>
        </tr>
        </thead>

        <tbody>
          {deliveryNodes}
        </tbody>
      </table>
    );
  }
});

var Delivery = React.createClass({
  render: function() {
    if (this.props.items != null){
      var items = this.props.items.map(function(item) {
        return (
          item.title
        );
      });
    }
    return (
      <tr>
        <td>
            {items != null ? items.join(" | ") : ''}
        </td>
        <td>
            {this.props.date != null ? moment(this.props.date).format('llll') : ''}
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
  <DeliveryBox url="api/deliveries" />,
  document.getElementById('content')
);
