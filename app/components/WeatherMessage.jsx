const React = require('react');

const WeatherMessage = ({temp, location}) => <div>Weather in {location} is {temp}!</div>;

module.exports = WeatherMessage;