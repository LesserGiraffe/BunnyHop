
(function() {
	return ['plink', '-ssh', ipAddr, '-l', uname, '-pw', password,
			'java', '-jar', ('-Djava.rmi.server.hostname=' + ipAddr), ('~/BunnyHop/' + execEnvironment), 'false'];
})();