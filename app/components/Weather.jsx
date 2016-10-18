const React = require('react');
const WeatherForm = require('./WeatherForm');
const WeatherMessage = require('./WeatherMessage');
const openWeatherMap = require('./../api/openWeatherMap');

const Weather = React.createClass({

    getInitialState: function () {
        return {
            isLoading: false
        }
    },

    handleSearch: function (location) {
        const that = this;

        this.setState({
            isLoading: true
        });

        openWeatherMap.getTemp(location).then(function (temp) {
            that.setState({
                location: location,
                temp: temp,
                isLoading: false
            });
        }, function (errorMessage) {
            that.setState({
                isLoading: false
            });
            alert(errorMessage);
        });
    },

    render: function () {

        const {isLoading, location, temp} = this.state;

        function renderMessage() {
            if (isLoading) {
                return <h3>Fetching data...</h3>;
            } else if (location && temp) {
                return <WeatherMessage location={location} temp={temp}/>;
            }
        }

        return (
            <div>
                <h3>Weather Compoenet!</h3>
                <WeatherForm onSearch={this.handleSearch}/>
                {renderMessage()}
            </div>
        );
    }
});

module.exports = Weather;