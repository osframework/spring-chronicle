#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "osframework/spring-chronicle" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
	echo -e "Publishing site...\n"

	cp -R target/site $TRAVIS_BUILD_DIR/site-latest

	cd $TRAVIS_BUILD_DIR
	git config --global user.email "travis@travis-ci.org"
	git config --global user.name "travis-ci"
	git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/osframework/spring-chronicle gh-pages > /dev/null

	cd gh-pages
	git rm -rf ./apidocs
	git rm -rf ./css
	git rm -rf ./images
	git rm -rf ./testapidocs
	git rm -rm ./*.html
	cp -Rf $TRAVIS_BUILD_DIR/site-latest ./
	git add -f .
	git commit -m "Latest site generated on successful build $TRAVIS_BUILD_NUMBER"
	git push -fq origin gh-pages > /dev/null

	echo -e "Published site to gh-pages.\n"
fi
