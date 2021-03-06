var UserForm = React.createClass({
  handleSubmit: function (event) {
    event.preventDefault();
    $.ajax({
      url: this.props.url,
      type: 'PATCH',
      data: JSON.stringify({
        "deliveryEmail": this.state.deliveryEmail,
        "notifyNoArticles": this.state.notifyNoArticles
      }),
      contentType: "application/json; charset=utf-8",
      cache: false,
      success: function(data) {
        this.setState({
          success: true,
          error: false,
          deliverySender: data.deliverySender
        })
      }.bind(this),
      error: function(xhr, status, err) {
        this.handleDeliveryError(xhr.responseJSON.description)
      }.bind(this)
    });
  },
  handleEMailChange: function(event) {
    console.log(event);
    this.setState({
      deliveryEmail: event.target.value
    });
  },
  handleNotifyNoArticlesChange: function(event) {
    console.log(event.target.checked);
    this.setState({
      notifyNoArticles: event.target.checked
    });
  },
  handleDeliveryError: function(description) {
     this.setState({success: false, error: true, errorDescription: description})
  },
  getInitialState: function() {
    return {deliveryEmail: '', notifyNoArticles: false, error: false, success: false};
  },
  componentDidMount: function() {
   $.get(this.props.url, function(data) {
      if (this.isMounted()) {
        this.setState({
          deliveryEmail: data.deliveryEmail,
          deliverySender: data.deliverySender,
          notifyNoArticles: data.notifyNoArticles,
        });
      }
    }.bind(this));
  },
  render: function() {
    return (
     <div className="container">
      {this.state.error == true ? <div className='error_div'>{this.state.errorDescription}</div> : ''}
      {this.state.success == true ? <div className='success_div'>Done!</div> : ''}
      {this.state.deliverySender != null ? <div className='info_div'>Make sure to add <b>{this.state.deliverySender}</b> to your <b>Approved Personal Document E-mail List</b>, you can do it <a href='https://www.amazon.com/mn/dcw/myx.html/ref=kinw_myk_surl_2#/home/settings/' target="_blank">here</a>.</div> : '' }
      {this.state.deliveryEmail != null && this.state.deliverySender == null ?
        <div className='error_div'>Your personal sender email address is not set. Click <b>Save</b> to create it. Remember to add it to Approved Emails in Amazon settings afterwards.</div>
        : '' }
      <form className="col s12" id="settings_form" method="POST" onSubmit={this.handleSubmit}>
          <div className="row">
              <div className="input-field col s12">
                <input value={this.state.deliveryEmail} onChange={this.handleEMailChange} name="email" id="email" type="email" className="validate"/>
                <label className="active" htmlFor="email">Send-to-Kindle E-Mail</label>
            </div>
          </div>
          <div className="row">
              <div className="input-field col s12">
              {this.state.notifyNoArticles == true ?
                <input checked='checked' onChange={this.handleNotifyNoArticlesChange} name="notifyNoArticles" id="notifyNoArticles" type="checkbox"/>
                  :
                <input onChange={this.handleNotifyNoArticlesChange} name="notifyNoArticles" id="notifyNoArticles" type="checkbox"/>
              }
              <label className="active" htmlFor="notifyNoArticles">Notify me by email in case there was no articles to send in scheduled delivery</label>
            </div>
          </div>
          <button className="btn waves-effect waves-light" type="submit" name="action">Save
              <i className="material-icons right">send</i>
          </button>
      </form>
      </div>
    );
  }
});

ReactDOM.render(
  <UserForm url="api/users/self"/>,
  document.getElementById('content')
);
