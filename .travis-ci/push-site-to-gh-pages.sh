#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "osframework/spring-chronicle" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ] || [ "$TRAVIS_BRANCH" == "develop" ]; then
	echo -e "Publishing site...\n"

	cp -R target/site $HOME/site-latest

	cd $HOME
	git config --global user.email "travis@travis-ci.org"
	git config --global user.name "travis-ci"
	git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/osframework/spring-chronicle gh-pages > /dev/null

	echo -e "Cloned branch 'gh-pages' to ${HOME}/gh-pages\n"

	cd gh-pages
	if [ -d ./apidocs ]; then
		git rm -rf ./apidocs
	fi
	if [ -d ./css ]; then
		git rm -rf ./css
	fi
	if [ -d ./images ]; then
		git rm -rf ./images
	fi
	if [ -d ./testapidocs ]; then
		git rm -rf ./testapidocs
	fi
	git rm -rf ./*.html
	cp -Rf $HOME/site-latest/apidocs ./apidocs
	cp -Rf $HOME/site-latest/css ./css
	cp -Rf $HOME/site-latest/images ./images
	cp $HOME/site-latest/*.html ./
	git add -f .
	git commit -m "Latest site generated on successful build $TRAVIS_BUILD_NUMBER"
	git push -fq origin gh-pages > /dev/null

	echo -e "Published site to gh-pages.\n"
fi
