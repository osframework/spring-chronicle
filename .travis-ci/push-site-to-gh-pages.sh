#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "osframework/spring-chronicle" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
	echo -e "Publishing site...\n"

	echo -e "Published site to gh-pages.\n"
fi
